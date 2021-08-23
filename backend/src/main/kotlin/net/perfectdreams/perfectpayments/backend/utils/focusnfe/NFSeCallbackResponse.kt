package net.perfectdreams.perfectpayments.backend.utils.focusnfe

import kotlinx.serialization.Serializable

@Serializable
data class NFSeCallbackResponse(
    val ref: String,
    val status: String,
    val url: String? = null
)