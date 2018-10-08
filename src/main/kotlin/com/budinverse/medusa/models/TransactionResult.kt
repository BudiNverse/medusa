package com.budinverse.medusa.models

sealed class TransactionResult {
    class Ok(val res: Any? = null) : TransactionResult()
    class Err(val e: Exception? = null) : TransactionResult()
}


