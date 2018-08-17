package com.budinverse.medusa.utils

sealed class TransactionResult {
    class Success : TransactionResult()
    class Fail(val e: Exception? = null) : TransactionResult()
}


