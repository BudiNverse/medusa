package com.budinverse.medusa.models


sealed class ExecResult<T> {
    class SingleResult<T>(val rowsMutated: Int, val transformed: T? = null) : ExecResult<T>()
    class BatchResult<T>(val rowsMutatedArr: IntArray, val transformed: T? = null) : ExecResult<T>()
    class ListResult<T>(val transformedList: ArrayList<T> = arrayListOf()) : ExecResult<T>()
}

