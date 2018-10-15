package com.budinverse.medusa.config

import java.io.FileInputStream
import java.util.*

/**
 * Sets your config
 * Creates a DatabaseConfig object based on user
 * specified type ie.
 * - databaseUser
 * - databasePassword
 * - databaseUrl
 * - driver
 * @param block Users can set parameters for DatabaseConfig in this block
 */
fun dbConfig(block: DatabaseConfig.() -> Unit) {
    val config = DatabaseConfig()
    block(config)
    DatabaseConfig.databaseConfig = config
}


/**
 * @constructor Creates a DatabaseConfig object
 * @property databaseUser User for your database
 * @property databasePassword Password for your database
 * @property databaseUrl URL to access your database
 * @property driver Database driver
 * @property generatedKeySupport Whether database supports generatedKey
 */
class DatabaseConfig(var databaseUser: String? = null,
                     var databasePassword: String? = null,
                     var databaseUrl: String? = null,
                     var driver: String? = null,
                     var generatedKeySupport: Boolean = true) {
    companion object {
        /**
         * Uninitialised DatabaseConfig
         * Has to be initialised if being used ಠ_ಠ
         */
        lateinit var databaseConfig: DatabaseConfig

        /**
         * Sets database config using a properties file
         * @param configFileDir Directory where the config file lives. Format is .properties
         * Default file name is "databaseConfig.properties"
         */
        fun setConfig(configFileDir: String = "databaseConfig.properties") {
            val properties = Properties()
            val input = FileInputStream(configFileDir)

            properties.load(input)

            // creates a the configuration from a file
            databaseConfig = DatabaseConfig(databaseUser = properties.getProperty("databaseUser"),
                    databasePassword = properties.getProperty("databasePassword"),
                    databaseUrl = properties.getProperty("databaseUrl"),
                    driver = properties.getProperty("driver"),
                    generatedKeySupport = properties.getProperty("generatedKeySupport").toBoolean())
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