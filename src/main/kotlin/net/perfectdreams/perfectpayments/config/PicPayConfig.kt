package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable

@Serializable
class PicPayConfig(
    val token: String,
    val seller: String
)