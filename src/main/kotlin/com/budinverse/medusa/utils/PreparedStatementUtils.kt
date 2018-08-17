package com.budinverse.medusa.utils

import java.sql.PreparedStatement

operator fun <T> PreparedStatement.set(index: Int, data: T): Unit = this.setObject(index, data)