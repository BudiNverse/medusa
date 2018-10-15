package com.budinverse.medusa.models


data class ExecResult<T>(val rowsMutated: Int, val transformed: T? = null)

