package net.perfectdreams.perfectpayments.backend.config

import kotlinx.serialization.Serializable

@Serializable
class PicPayConfig(
    val token: String,
    val seller: String
)