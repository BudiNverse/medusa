package com.budinverse.medusa.core

import com.budinverse.medusa.config.DatabaseConfig.Companion.databaseConfig
import com.budinverse.medusa.models.ExecBuilder
import com.budinverse.medusa.models.ExecResult
import com.budinverse.medusa.models.QueryBuilder
import com.budinverse.medusa.models.TransactionResult
import com.budinverse.medusa.models.TransactionResult.Err
import com.budinverse.medusa.models.TransactionResult.Ok
import com.budinverse.medusa.utils.*
import kotlinx.coroutines.experimental.async
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
    TransactionBuilder(block = block).run {
        return try {
            block()
            Ok(this.results)
        } catch (e: Exception) {
            e.printStackTrace()
            connection.rollback()
            Err(e)
        } finally {
            finalize()
        }
    }
}

/**
 * Asynchronous version of transaction
 * @see transaction
 * @param block Takes in functions that are available in TransactionBuilder ie.
 * - exec
 * - insert
 * - update
 * - delete
 * - query
 * - queryList
 * @return Deferred [TransactionResult]
 */
fun transactionAsync(block: TransactionBuilder.() -> Unit) = async {
    transaction {
        block()
    }
}

class TransactionBuilder constructor(
        val connection: Connection = getDatabaseConnection(),
        val block: TransactionBuilder.() -> Unit) {

    internal val results: MutableList<Any?> = arrayListOf()

    private val pss: MutableList<PreparedStatement> = mutableListOf()

    init {
        connection.autoCommit = false
    }

    /**
     * DSL version of [exec]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun exec(block: ExecBuilder.() -> Unit) {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        when (execBuilder.hasPreparedStatement) {
            true -> exec(execBuilder.statement, execBuilder.values)
            else -> exec(execBuilder.statement)
        }
    }

    /**
     * DSL version of [insert]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun insert(block: ExecBuilder.() -> Unit) {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        insert(execBuilder.statement, execBuilder.values)
    }

    /**
     * DSL version of [update]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun update(block: ExecBuilder.() -> Unit) {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        insert(execBuilder.statement, execBuilder.values)
    }

    /**
     * DSL version of [delete]
     * Same implementation as [exec]. Created to improve readability
     * @param block Block that sets [ExecBuilder] and uses it for operations
     */
    fun delete(block: ExecBuilder.() -> Unit) {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        insert(execBuilder.statement, execBuilder.values)
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
                ?: throw IllegalArgumentException("Type with `resultSet` constructor not provided!")
    }

    /**
     * Executes raw SQL String without using any preparedStatements
     * @param statement
     */
    fun exec(statement: String) {
        val rowsMutated = connection.prepareStatement(statement).executeUpdate()
        results.add(ExecResult(rowsMutated))
    }

    fun exec(statement: String, psValues: Array<Any?> = arrayOf()) {
        val ps: PreparedStatement = if (databaseConfig.generatedKeySupport)
            connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
        else
            connection.prepareStatement(statement)

        pss += ps

        // check for parameter size and values provided
        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        // set resultSet to preparedStatement from psValues
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val rowsMutated = ps.executeUpdate()
        val rs: ResultSet = ps.generatedKeys

        when {
            databaseConfig.generatedKeySupport && rs.next() -> results.add(ExecResult(rowsMutated, rs))
            else -> results.add(ExecResult(rowsMutated))
        }
    }

    fun <T> query(statement: String, psValues: Array<Any?> = arrayOf(), transform: (ResultSet) -> T) {
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
            resultSet.next() -> results.add(transform(resultSet))
        }

    }

    fun <T> queryList(statement: String, psValues: Array<Any?> = arrayOf(), transform: (ResultSet) -> T) {
        val list = mutableListOf<T>()
        val ps = connection.prepareStatement(statement)
        pss += ps

        require(ps.parameterMetaData.parameterCount == psValues.size) {
            "Number of values does not match number of parameters in preparedStatement!"
        }

        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        val resultSet: ResultSet = ps.executeQuery()
        while (resultSet.next())
            list.add(transform(resultSet))

        results.add(list)
    }

    fun insert(statement: String, psValues: Array<Any?> = arrayOf()) = exec(statement, psValues)
    fun update(statement: String, psValues: Array<Any?> = arrayOf()) = exec(statement, psValues)
    fun delete(statement: String, psValues: Array<Any?> = arrayOf()) = exec(statement, psValues)

    fun finalize() {
        pss.forEach { println("\u001B[36m[medusa]: $it. Warning(s): ${it.warnings}. RowsUpdated: ${it.updateCount} \u001B[0m") }
        connection.commit()
        pss.map(PreparedStatement::close)
        connection.close()
        println("\u001B[33m[medusa]: Closing connection: ${this.connection} \u001B[0m")
    }
}