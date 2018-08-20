package com.budinverse.medusa.utils

import java.sql.ResultSet

sealed class QueryResult<out R, out D> {
    class DataOnly<out D>(val data: D) : QueryResult<Nothing, D>()
    class All<out R, out D>(val resultSet: R, val data: D) : QueryResult<ResultSet, D>()
    class ResultSetOnly<out R>(val resultSet: R) : QueryResult<ResultSet, Nothing>()
    class Empty : QueryResult<Nothing, Nothing>()
}
