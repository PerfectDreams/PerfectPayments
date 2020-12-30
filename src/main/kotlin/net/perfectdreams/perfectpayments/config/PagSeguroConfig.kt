package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable

@Serializable
class PagSeguroConfig(
    val email: String,
    val token: String,
    val notificationUsername: String,
    val notificationPassword: String
)