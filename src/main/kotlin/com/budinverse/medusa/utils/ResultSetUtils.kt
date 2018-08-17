package com.budinverse.medusa.utils

import java.sql.ResultSet

operator fun <T> ResultSet.get(index: Int): T = this.getObject(index) as T
operator fun <T> ResultSet.get(columnName: String): T = this.getObject(columnName) as T
operator fun <T> ResultSet.get(columnName: String, type: Class<T>): T = this.getObject(columnName, type)
