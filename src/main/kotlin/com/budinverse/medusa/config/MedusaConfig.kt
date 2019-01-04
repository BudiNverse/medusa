package com.budinverse.medusa.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.FileInputStream
import java.util.*

/**
 * Sets your config
 * Creates a MedusaConfig object based on user
 * specified type ie.
 * - databaseUser
 * - databasePassword
 * - databaseUrl
 * - driver
 * @param block Users can set parameters for MedusaConfig in this block
 */
fun dbConfig(block: MedusaConfig.() -> Unit) {
    val config = MedusaConfig()
    block(config)
    MedusaConfig.medusaConfig = config
}

class MedusaConfig(var databaseUser: String = "",
                   var databasePassword: String = "",
                   var databaseUrl: String = "",
                   var driver: String = "",
                   var generatedKeySupport: Boolean = true) {

    lateinit var connectionPool: HikariDataSource

    /**
     * Creates a HikariDataSource object
     * @param block
     * @return [HikariDataSource]
     */
    fun connectionPool(block: HikariConfig.() -> Unit): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = this@MedusaConfig.databaseUrl
            username = this@MedusaConfig.databaseUser
            password = this@MedusaConfig.databasePassword
            //isAutoCommit = false
        }

        block(hikariConfig)
        return HikariDataSource(hikariConfig)
    }

    companion object {
        /**
         * Uninitialised MedusaConfig
         * Has to be initialised if being used ಠ_ಠ
         */
        lateinit var medusaConfig: MedusaConfig

        /**
         * Sets database config using a properties file
         * @param configFileDir Directory where the config file lives. Format is .properties
         * Default file name is "medusaConfig.properties"
         */
        fun setConfig(configFileDir: String = "medusaConfig.properties") {
            val properties = Properties()
            val input = FileInputStream(configFileDir)

            properties.load(input)

            // creates a the configuration from a file
            medusaConfig = MedusaConfig(databaseUser = properties.getProperty("databaseUser"),
                    databasePassword = properties.getProperty("databasePassword"),
                    databaseUrl = properties.getProperty("databaseUrl"),
                    driver = properties.getProperty("driver"),
                    generatedKeySupport = properties.getProperty("generatedKeySupport").toBoolean())
        }

        /**
         * Sets a database config programmatically
         * @param medusaConfig
         */
        fun setConfig(medusaConfig: MedusaConfig) {
            this.medusaConfig = medusaConfig
        }
    }
}