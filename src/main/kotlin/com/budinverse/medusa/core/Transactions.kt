package com.budinverse.medusa.core

import com.budinverse.medusa.utils.*
import com.budinverse.medusa.utils.TransactionResult.Fail
import com.budinverse.medusa.utils.TransactionResult.Success
import kotlinx.coroutines.experimental.async
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

typealias ExecRKeys = ExecResult<Int, ResultSet>
typealias ExecRKeyless = ExecResult<Int, Nothing>

/**
 * Starts a transaction
 * Automatically opens databaseConnection and closes it when all operations are done
 * @param block Takes in functions that are available in TransactionBuilder ie.
 * - exec
 * - execKeys
 * - insert
 * - insertKeys
 * - query
 * - queryList
 * @return [TransactionResult]
 * @author BudiNverse [ budisyahiddin@pm.me ]
 */
fun transaction(block: TransactionBuilder.() -> Unit): TransactionResult {
    TransactionBuilder(block = block).run {
        return try {
            block()
            this.finalize()

            Success()
        } catch (e: Exception) {
            e.printStackTrace()
            connection.rollback()
            Fail(e)
        }
    }
}

/**
 * Asynchronous version of transaction
 * @see transaction
 * @param block Takes in functions that are available in TransactionBuilder ie.
 * - exec
 * - execKeys
 * - insert
 * - insertKeys
 * - query
 * - queryList
 * @return Deferred [TransactionResult]
 * @author BudiNverse [ budisyahiddin@pm.me ]
 */
fun transactionAsync(block: TransactionBuilder.() -> Unit) = async {
    transaction {
        block()
    }
}

class TransactionBuilder constructor(
        val connection: Connection = getDatabaseConnection(),
        val block: TransactionBuilder.() -> Unit) {

    private val pss: MutableList<PreparedStatement> = mutableListOf()

    init {
        connection.autoCommit = false
    }

    /**
     * DSL version of [exec]
     * @param block Block that sets [ExecBuilder] and uses it for operations
     * @return [Any]
     * @author BudiNverse [ budisyahiddin@pm.me ]
     */
    fun exec(block: ExecBuilder.() -> Unit): ExecRKeyless {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return if (execBuilder.hasPreparedStatement) {
            exec(execBuilder.statement, execBuilder.values)
        } else {
            exec(execBuilder.statement)
        }
    }

    fun execKeys(block: ExecBuilder.() -> Unit): ExecRKeys {
        val execBuilder = ExecBuilder()
        block(execBuilder)
        return execKeys(execBuilder.statement, execBuilder.values)
    }

    fun insert(block: ExecBuilder.() -> Unit): ExecRKeys {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return insert(execBuilder.statement, execBuilder.values)
    }

    fun insertKeys(block: ExecBuilder.() -> Unit): ExecRKeys {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return insertKeys(execBuilder.statement, execBuilder.values)
    }

    fun <T> query(block: QueryBuilder<T>.() -> Unit): QueryResult<ResultSet, T> {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        return queryBuilder.type?.let { query(queryBuilder.statement, queryBuilder.values, it) }
                ?: QueryResult.Empty()
    }

    fun <T> queryList(block: QueryBuilder<T>.() -> Unit): QueryResult<ResultSet, List<T>> {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        return queryBuilder.type?.let { queryList(queryBuilder.statement, queryBuilder.values, it) }
                ?: QueryResult.DataOnly(listOf())
    }

    /**
     * Executes raw SQL String without using any preparedStatements
     * @param statement
     */
    fun exec(statement: String): ExecRKeyless {
        val rowsMutated = connection.prepareStatement(statement).executeUpdate()
        return ExecResult.RowsMutated(rowsMutated)
    }

    fun exec(statement: String, psValues: Array<Any?> = arrayOf()): ExecRKeyless {
        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val rowsMutated = ps.executeUpdate()
        return ExecResult.RowsMutated(rowsMutated)
    }

    fun execKeys(statement: String, psValues: Array<Any?> = arrayOf()): ExecRKeys {
        val ps: PreparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val rowsMutated = ps.executeUpdate()
        val rs: ResultSet = ps.generatedKeys



        return if (rs.next()) ExecResult.WithKeys(rowsMutated, rs)
        else ExecResult.RowsMutated(rowsMutated)
    }

    fun <T> query(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T):
            QueryResult<ResultSet, T> {
        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val resultSet: ResultSet = ps.executeQuery()

        return if (resultSet.next()) {
            QueryResult.All(resultSet, block(resultSet))
        } else {
            QueryResult.ResultSetOnly(resultSet)
        }
    }

    fun <T> queryList(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T):
            QueryResult<ResultSet, List<T>> {
        val list = mutableListOf<T>()
        val ps = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        val resultSet: ResultSet = ps.executeQuery()
        while (resultSet.next())
            list.add(block(resultSet))

        return QueryResult.All(resultSet, list)
    }

    fun insert(statement: String, psValues: Array<Any?> = arrayOf()) = exec(statement, psValues)
    fun insertKeys(statement: String, psValues: Array<Any?> = arrayOf()) = execKeys(statement, psValues)

    fun finalize() {
        connection.commit()
        pss.map(PreparedStatement::close)
        connection.close()
    }
}