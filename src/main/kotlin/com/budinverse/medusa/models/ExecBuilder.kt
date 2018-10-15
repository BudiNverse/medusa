package com.budinverse.medusa.models

import java.sql.ResultSet

open class ExecBuilder<T>(var statement: String = "",
                          var values: Array<Any?> = arrayOf(),
                          var hasPreparedStatement: Boolean = true,
                          open var type: ((ResultSet) -> T)? = null)

