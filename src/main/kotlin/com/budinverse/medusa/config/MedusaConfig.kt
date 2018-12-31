package com.budinverse.medusa.config

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


/**
 * @constructor Creates a MedusaConfig object
 * @property databaseUser User for your database
 * @property databasePassword Password for your database
 * @property databaseUrl URL to access your database
 * @property driver Database driver
 * @property generatedKeySupport Whether database supports generatedKey
 */
class MedusaConfig(var databaseUser: String? = null,
                   var databasePassword: String? = null,
                   var databaseUrl: String? = null,
                   var driver: String? = null,
                   var generatedKeySupport: Boolean = true) {
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