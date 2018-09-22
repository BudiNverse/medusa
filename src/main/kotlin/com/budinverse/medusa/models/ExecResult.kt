package com.budinverse.medusa.models

import java.sql.ResultSet

//sealed class ExecResult {
//    resultSet class RowsMutated(val rowsMutated: Int) : ExecResult()
//    resultSet class AllExec(val rowsMutated: Int, val value: ResultSet) : ExecResult()
//}

data class ExecResult(val rowsMutated: Int, val resultSet: ResultSet? = null)

