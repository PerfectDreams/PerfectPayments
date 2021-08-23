package net.perfectdreams.perfectpayments.backend.config

import kotlinx.serialization.Serializable

@Serializable
data class WebsiteConfig(
    val url: String,
    val host: String,
    val port: Int,
    val dataFolder: String
)