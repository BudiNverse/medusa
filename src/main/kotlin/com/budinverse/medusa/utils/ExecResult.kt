package com.budinverse.medusa.utils

sealed class ExecResult<out R, out V> {
    class RowsMutated<out R>(val rowsMutated: R) : ExecResult<R, Nothing>()
    class WithKeys<out R, out V>(val rowsMutated: R, val value: V) : ExecResult<R, V>()
}