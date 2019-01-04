package com.budinverse.medusa.core

import com.budinverse.medusa.config.MedusaConfig
import com.budinverse.medusa.config.MedusaConfig.Companion.medusaConfig
import com.budinverse.medusa.models.ExecBuilder
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.models.QueryBuilder
import com.budinverse.medusa.models.TransactionResult
import com.budinverse.medusa.models.TransactionResult.Err
import com.budinverse.medusa.models.TransactionResult.Ok
import com.budinverse.medusa.utils.*
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement


/**
 * Starts a transaction
 * Automatically opens databaseConnection and closes it when all operations are done
 * @param block Takes in functions that are available in TransactionBuilder ie.
 * - exec
 * - insert
 * - update
 * - delete
 * - query
 * - queryList
 * @return [TransactionResult]
 */
fun transaction(block: TransactionBuilder.() -> Unit): TransactionResult {
    TransactionBuilder().run {
        return try {
            block()
            Ok(results)
        } catch (e: Exception) {
            Err(e)
        } finally {
            finalize()
        }
    }
}

/**
 * Asynchronous version of transaction
 * @see transaction
 * @param dispatcher Coroutine Dispatcher, default is Dispatchers.IO
 * @param block Takes in functions that are available in TransactionBuilder ie.
 * - exec
 * - insert
 * - update
 * - delete
 * - query
 * - queryList
 * @return Deferred [TransactionResult]
 */
fun transactionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO,
                     block: TransactionBuilder.() -> Unit): Deferred<TransactionResult> =

        CoroutineScope(dispatcher).async {
            transaction {
                block()
            }
        }

class TransactionBuilder constructor(
        val connection: Connection = MedusaConfig.medusaConfig.connectionPool.connection) {

    internal val results: ArrayList<Any?> = arrayListOf()

    private val pss: ArrayList<PreparedStatement> = arrayListOf()

    /**
     * DSL version of [exec]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> exec(block: ExecBuilder<T>.() -> Unit) {
        val execBuilder = ExecBuilder<T>()
        block(execBuilder)

        when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values, execBuilder.type)
            else -> exec<T>(execBuilder.statement)
        }
    }

    /**
     * DSL version of [insert]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> insert(block: ExecBuilder<T>.() -> Unit) {
        val execBuilder = ExecBuilder<T>()
        block(execBuilder)

        exec(execBuilder.statement, execBuilder.values, execBuilder.type)
    }

    /**
     * DSL version of [update]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> update(block: ExecBuilder<T>.() -> Unit) {
        val execBuilder = ExecBuilder<T>()
        block(execBuilder)

        exec(execBuilder.statement, execBuilder.values, execBuilder.type)
    }

    /**
     * DSL version of [delete]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> delete(block: ExecBuilder<T>.() -> Unit) {
        val execBuilder = ExecBuilder<T>()
        block(execBuilder)

        exec(execBuilder.statement, execBuilder.values, execBuilder.type)
    }

    /**
     * DSL version of [query]
     * @param block Block that sets [QueryBuilder] and uses it for operations
     */
    fun <T> query(block: QueryBuilder<T>.() -> Unit) {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        queryBuilder.type?.let { query(queryBuilder.statement, queryBuilder.values, it) }
    }

    /**
     * DSL version of [queryList]
     * @param block Block that sets [QueryBuilder] and uses it for operations
     */
    fun <T> queryList(block: QueryBuilder<T>.() -> Unit) {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        queryBuilder.type?.let { queryList(queryBuilder.statement, queryBuilder.values, it) }
                ?: throw IllegalArgumentException("Type with `type` constructor not provided!")
    }

    /**
     * Executes raw SQL String without using any [PreparedStatement]
     * @param statement
     */
    private fun <T> exec(statement: String) {
        val rowsMutated = connection.prepareStatement(statement).executeUpdate()
        results.add(ExecResult<T>(rowsMutated))
    }

    private fun <T> exec(statement: String, psValues: Array<Any?> = arrayOf(), transform: ((ResultSet) -> T)? = null) {
        val ps: PreparedStatement = when (medusaConfig.generatedKeySupport) {
            true -> connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
            else -> connection.prepareStatement(statement)
        }

        pss += ps

        // check for parameter size and values provided
        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        // set type to preparedStatement from psValues
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val rowsMutated: Int = ps.executeUpdate()
        val resultSet: ResultSet = ps.generatedKeys

        when {
            medusaConfig.generatedKeySupport && resultSet.next() ->
                results.add(ExecResult(rowsMutated, transform?.invoke(resultSet))).run { resultSet.close() }
            else -> results.add(ExecResult<T>(rowsMutated)).run { resultSet.close() }
        }
    }

    private fun <T> query(statement: String, psValues: Array<Any?> = arrayOf(), transform: (ResultSet) -> T) {
        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps

        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        val resultSet: ResultSet = ps.executeQuery()

        when {
            resultSet.next() -> results.add(transform(resultSet)).run { resultSet.close() }
            else -> results.add(null).run { resultSet.close() }
        }

    }

    private fun <T> queryList(statement: String, psValues: Array<Any?> = arrayOf(), transform: (ResultSet) -> T) {
        val list: ArrayList<T> = arrayListOf()
        val ps: PreparedStatement = connection.prepareStatement(statement)
        lateinit var resultSet: ResultSet
        pss += ps

        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        resultSet = ps.executeQuery()

        while (resultSet.next())
            list.add(transform(resultSet))

        results.add(list)
        resultSet.close()
    }

    internal fun finalize() {
        pss.forEach { println("\u001B[36m[medusa]\u001B[0m: $it. Warning(s): ${it.warnings}. RowsUpdated: ${it.updateCount}") }
        pss.map(PreparedStatement::close)
        println("\u001B[33m[medusa]\u001B[0m: Returning connection: ${this.connection}")
        connection.close()
    }
}