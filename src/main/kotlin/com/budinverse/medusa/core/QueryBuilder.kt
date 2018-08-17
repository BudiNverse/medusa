package com.budinverse.medusa.core

import java.sql.ResultSet

class QueryBuilder<T>(var type: ((ResultSet) -> T)? = null) : ExecBuilder()

