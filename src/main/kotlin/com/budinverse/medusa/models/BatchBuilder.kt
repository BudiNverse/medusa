package com.budinverse.medusa.models

import java.sql.ResultSet

class BatchBuilder<T>(var statement: String = "",
                      var values: Array<Array<Any?>> = arrayOf(),
                      var type: ((ResultSet) -> T)? = null)