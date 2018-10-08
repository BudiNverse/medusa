package com.budinverse.medusa.models

open class ExecBuilder(var statement: String = "",
                       var values: Array<Any?> = arrayOf(),
                       var hasPreparedStatement: Boolean = true)

