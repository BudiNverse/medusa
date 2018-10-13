package com.budinverse.medusa.models

import java.sql.ResultSet

data class ExecResult(val rowsMutated: Int, val resultSet: ResultSet? = null)

