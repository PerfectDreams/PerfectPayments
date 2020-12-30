package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable

@Serializable
class DatabaseConfig(
        val databaseName: String,
        val address: String,
        val username: String,
        val password: String? = null
)