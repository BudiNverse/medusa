package com.budinverse.medusa.models

import java.sql.ResultSet

class QueryBuilder<T>(override var type: ((ResultSet) -> T)? = null) : ExecBuilder<T>()

