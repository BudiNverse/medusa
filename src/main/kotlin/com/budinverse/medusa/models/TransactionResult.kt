package com.budinverse.medusa.models

sealed class TransactionResult {
    class Ok(val res: ArrayList<Any?> = arrayListOf()) : TransactionResult()
    class Err(val e: Throwable? = null) : TransactionResult()
}


