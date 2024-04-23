package net.perfectdreams.perfectpayments.backend.config

import kotlinx.serialization.Serializable

@Serializable
class MercadoPagoConfig(
    val accessToken: String,
    val webhookSecretSignature: String,
    val callbackUrl: String
)