package net.perfectdreams.perfectpayments.utils.focusnfe

import kotlinx.serialization.Serializable

@Serializable
data class NFSeCallbackResponse(
    val ref: String,
    val status: String
)