package com.budinverse.medusa.utils

import com.budinverse.medusa.config.DatabaseConfig.Companion.databaseConfig
import java.sql.Connection
import java.sql.DriverManager

/**
 * Gets the database connection based on the config file provided
 * @return Connection
 */
fun getDatabaseConnection(): Connection {
    val jdbcDriver = databaseConfig.driver
    val dbUrl = databaseConfig.databaseUrl
    Class.forName(jdbcDriver)
    return DriverManager.getConnection(dbUrl, databaseConfig.databaseUser, databaseConfig.databasePassword.toString())
}