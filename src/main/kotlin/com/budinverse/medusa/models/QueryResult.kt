package com.budinverse.medusa.models


sealed class QueryResult {
    class Ok<D, T>(val data: D, transformedData: T) : QueryResult()
    class Err<E>(val e: E) : QueryResult()

    class NoDataReturned(override val message: String = "") : Exception()
    class EmptyList(override val message: String = "") : Exception()
    class MissingType(override val message: String = "") : Exception()
}


