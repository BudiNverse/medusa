package com.budinverse.medusa.models

/**
 * @property rowsMutated No of rows that was mutated
 * @property transformed data that was transformed
 */
data class ExecResult<T>(val rowsMutated: Int, val transformed: T? = null)

