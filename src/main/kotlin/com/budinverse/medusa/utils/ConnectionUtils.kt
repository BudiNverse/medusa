package com.budinverse.medusa.utils

import com.budinverse.medusa.config.MedusaConfig.Companion.medusaConfig
import java.sql.Connection
import java.sql.DriverManager

/**
 * Gets the database connection based on the config file provided
 * @return Connection
 */
fun getDatabaseConnection(): Connection {
    Class.forName(medusaConfig.driver)
    return DriverManager.getConnection(medusaConfig.databaseUrl,
            medusaConfig.databaseUser,
            medusaConfig.databasePassword.toString())
}