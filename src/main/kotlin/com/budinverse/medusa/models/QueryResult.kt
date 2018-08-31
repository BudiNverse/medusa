package com.budinverse.medusa.models

import java.sql.ResultSet


sealed class QueryResult {
    class Ok<T>(val resultSet: ResultSet, transformedData: T) : QueryResult()
    class Err<E>(val e: E) : QueryResult()

    class NoDataReturned(override val message: String = "") : Exception()
    class EmptyList(override val message: String = "") : Exception()
    class MissingType(override val message: String = "") : Exception()
}


