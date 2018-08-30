package com.budinverse.medusa.models

import java.sql.ResultSet

class QueryBuilder<T>(var type: ((ResultSet) -> T)? = null) : ExecBuilder()

