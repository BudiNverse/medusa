package com.budinverse.medusa.models

sealed class ExecResult<out R, out V> {
    class RowsMutated<out R>(val rowsMutated: R) : ExecResult<R, Nothing>()
    class AllExec<out R, out V>(val rowsMutated: R, val value: V) : ExecResult<R, V>()
}
