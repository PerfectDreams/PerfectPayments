package net.perfectdreams.perfectpayments.backend.utils

import kotlinx.serialization.Serializable

@Serializable
data class APIError(val message: String)