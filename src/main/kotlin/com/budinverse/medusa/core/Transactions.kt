package com.budinverse.medusa.core

import com.budinverse.medusa.config.MedusaConfig
import com.budinverse.medusa.config.MedusaConfig.Companion.medusaConfig
import com.budinverse.medusa.models.BatchBuilder
import com.budinverse.medusa.models.ExecBuilder
import com.budinverse.medusa.models.ExecResult
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
 */
fun transaction(block: TransactionBuilder.() -> Unit) {
    TransactionBuilder().run {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            connection.rollback()
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
 * @return Deferred [Unit]
 */
fun transactionAsync(dispatcher: CoroutineDispatcher = Dispatchers.IO,
                     block: TransactionBuilder.() -> Unit): Deferred<Unit> =

        CoroutineScope(dispatcher).async {
            transaction {
                block()
            }
        }

class TransactionBuilder constructor(
        val connection: Connection = MedusaConfig.medusaConfig.connectionPool.connection) {

    private val pss: ArrayList<PreparedStatement> = arrayListOf()
    private val resultSetList: ArrayList<ResultSet> = arrayListOf()

    init {
        connection.autoCommit = false
    }

    /**
     * DSL version of [exec]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> exec(block: ExecBuilder<T>.() -> Unit): ExecResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values, execBuilder.type)
            else -> exec(execBuilder.statement)
        }
    }

    /**
     * DSL version of [insert]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> insert(block: ExecBuilder<T>.() -> Unit): ExecResult.SingleResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values, execBuilder.type)
            else -> exec(execBuilder.statement)
        }
    }

    /**
     * DSL version of [batch]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [BatchBuilder] and uses it for operations
     */
    fun <T> batch(block: BatchBuilder<T>.() -> Unit): ExecResult.BatchResult<T> {
        val batchBuilder: BatchBuilder<T> = BatchBuilder()
        block(batchBuilder)

        return batch(batchBuilder.statement, batchBuilder.values, batchBuilder.type)
    }

    /**
     * DSL version of [update]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> update(block: ExecBuilder<T>.() -> Unit): ExecResult.SingleResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values, execBuilder.type)
            else -> exec(execBuilder.statement)
        }
    }

    /**
     * DSL version of [delete]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> delete(block: ExecBuilder<T>.() -> Unit): ExecResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values, execBuilder.type)
            else -> exec(execBuilder.statement)
        }
    }

    /**
     * DSL version of [query]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> query(block: ExecBuilder<T>.() -> Unit): ExecResult.SingleResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return execBuilder.type?.let { query(execBuilder.statement, execBuilder.values, it) }
                ?: throw IllegalArgumentException("Type constructor not provided!")
    }

    /**
     * DSL version of [queryList]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun <T> queryList(block: ExecBuilder<T>.() -> Unit): ExecResult.ListResult<T> {
        val execBuilder: ExecBuilder<T> = ExecBuilder()
        block(execBuilder)

        return execBuilder.type?.let { queryList(execBuilder.statement, execBuilder.values, it) }
                ?: throw IllegalArgumentException("Type constructor not provided!")
    }

    private fun <T> batch(statement: String,
                  psValues: Array<Array<Any?>>,
                  transform: ((ResultSet) -> T)? = null): ExecResult.BatchResult<T>  {

        val ps: PreparedStatement = when (medusaConfig.generatedKeySupport) {
            true -> connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
            else -> connection.prepareStatement(statement)
        }

        pss += ps

        psValues.forEach {
            for (i in 1..it.size) {
                ps[i] = it[i - 1]
            }
            ps.addBatch()
        }

        val rowsMutated: IntArray = ps.executeBatch()
        val resultSet: ResultSet = ps.generatedKeys

        return when {
            medusaConfig.generatedKeySupport && resultSet.next() ->
                ExecResult.BatchResult(rowsMutated, transform?.invoke(resultSet))
            else -> ExecResult.BatchResult(rowsMutated)
        }
    }

    /**
     * Executes raw SQL String without using any [PreparedStatement]
     * @param statement
     */
    private fun <T> exec(statement: String): ExecResult.SingleResult<T> {
        val rowsMutated = connection.prepareStatement(statement).executeUpdate()
        return ExecResult.SingleResult(rowsMutated)
    }

    private fun <T> exec(statement: String,
                         psValues: Array<Any?> = arrayOf(),
                         transform: ((ResultSet) -> T)? = null): ExecResult.SingleResult<T> {

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

        return when {
            medusaConfig.generatedKeySupport && resultSet.next() ->
                ExecResult.SingleResult(rowsMutated, transform?.invoke(resultSet))
            else -> ExecResult.SingleResult(rowsMutated)
        }

    }

    private inline fun <T> query(statement: String,
                                 psValues: Array<Any?> = arrayOf(),
                                 transform: (ResultSet) -> T): ExecResult.SingleResult<T> {

        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps

        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        val resultSet: ResultSet = ps.executeQuery()

        return when {
            resultSet.next() -> ExecResult.SingleResult(0, transform(resultSet))
            else -> ExecResult.SingleResult(0)
        }
    }

    private inline fun <T> queryList(statement: String,
                                     psValues: Array<Any?> = arrayOf(),
                                     transform: (ResultSet) -> T): ExecResult.ListResult<T> {
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

        return ExecResult.ListResult(list)
    }

    fun finalize() {
        pss.forEach { println("\u001B[36m[medusa]\u001B[0m: $it. Warning(s): ${it.warnings}. RowsUpdated: ${it.updateCount}") }
        resultSetList.map(ResultSet::close)
        pss.map(PreparedStatement::close)
        println("\u001B[33m[medusa]\u001B[0m: Returning connection: ${this.connection}")
        connection.commit()
        connection.close()
    }
}