package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable

@Serializable
class StripeConfig(
    val publishToken: String,
    val secretToken: String,
    val webhookSecret: String
)