package com.budinverse.medusa.core

import com.budinverse.medusa.utils.TransactionResult
import com.budinverse.medusa.utils.TransactionResult.Fail
import com.budinverse.medusa.utils.TransactionResult.Success
import com.budinverse.medusa.utils.getDatabaseConnection
import com.budinverse.medusa.utils.set
import kotlinx.coroutines.experimental.async
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement


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

    fun exec(block: ExecBuilder.() -> Unit): Any {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return if (execBuilder.hasPreparedStatement) {
            exec(execBuilder.statement, execBuilder.values)
        } else {
            exec(execBuilder.statement)
        }
    }

    fun execKeys(block: ExecBuilder.() -> Unit): ResultSet? {
        val execBuilder = ExecBuilder()
        block(execBuilder)
        return execKeys(execBuilder.statement, execBuilder.values)
    }

    fun insert(block: ExecBuilder.() -> Unit) {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return insert(execBuilder.statement, execBuilder.values)
    }

    fun insertKeys(block: ExecBuilder.() -> Unit): ResultSet? {
        val execBuilder = ExecBuilder()
        block(execBuilder)

        return insertKeys(execBuilder.statement, execBuilder.values)
    }

    fun <T> query(block: QueryBuilder<T>.() -> Unit): T? {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        return queryBuilder.type?.let { query(queryBuilder.statement, queryBuilder.values, it) }
    }

    fun <T> queryList(block: QueryBuilder<T>.() -> Unit): List<T> {
        val queryBuilder = QueryBuilder<T>()
        block(queryBuilder)

        return queryBuilder.type?.let { queryList(queryBuilder.statement, queryBuilder.values, it) }
                ?: listOf()
    }

    /**
     * Executes raw SQL String without using any preparedStatements
     * @param statement
     */
    fun exec(statement: String) = connection.prepareStatement(statement).executeUpdate()

    fun exec(statement: String, psValues: Array<Any?> = arrayOf()) {
        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        ps.executeUpdate()
    }

    fun execKeys(statement: String, psValues: Array<Any?> = arrayOf()): ResultSet? {
        val ps: PreparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        ps.executeUpdate()
        val rs = ps.generatedKeys

        return if (rs.next()) rs else null
    }

    fun <T> query(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T): T? {
        val ps: PreparedStatement = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val resultSet = ps.executeQuery()
        return if (resultSet.next()) {
            block(resultSet)
        } else {
            null
        }
    }

    fun <T> queryList(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T): List<T> {
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

        return list
    }

    fun insert(statement: String, psValues: Array<Any?> = arrayOf()) = exec(statement, psValues)
    fun insertKeys(statement: String, psValues: Array<Any?> = arrayOf()) = execKeys(statement, psValues)

    fun finalize() {
        connection.commit()
        pss.map(PreparedStatement::close)
        connection.close()
    }
}