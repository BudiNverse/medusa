package com.budinverse.medusa.models

sealed class TransactionResult {
    class Ok(val res: MutableList<Any?> = mutableListOf()) : TransactionResult()
    class Err(val e: Exception? = null) : TransactionResult()
}


