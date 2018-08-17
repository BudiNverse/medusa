package com.budinverse.medusa.config

import java.io.FileInputStream
import java.util.*

fun dbConfig(block: DatabaseConfig.() -> Unit) {
    val config = DatabaseConfig()
    block(config)
    DatabaseConfig.databaseConfig = config
}


class DatabaseConfig(var databaseUser: String? = null,
                     var databasePassword: String? = null,
                     var databaseUrl: String? = null,
                     var driver: String? = null) {
    companion object {
        lateinit var databaseConfig: DatabaseConfig

        /**
         * Sets database config using a properties file
         * @param configFileDir Directory where the config file lives. Format is .properties
         */
        fun setConfig(configFileDir: String = "databaseConfig.properties") {
            val properties = Properties()
            val input = FileInputStream(configFileDir)

            properties.load(input)

            // creates a the configuration from a file
            databaseConfig = DatabaseConfig(databaseUser = properties.getProperty("databaseUser"),
                    databasePassword = properties.getProperty("databasePassword"),
                    databaseUrl = properties.getProperty("databaseUrl"),
                    driver = properties.getProperty("driver"))
        }

        /**
         * Sets a database config programmatically
         * @param databaseConfig
         */
        fun setConfig(databaseConfig: DatabaseConfig) {
            this.databaseConfig = databaseConfig
        }
    }
}