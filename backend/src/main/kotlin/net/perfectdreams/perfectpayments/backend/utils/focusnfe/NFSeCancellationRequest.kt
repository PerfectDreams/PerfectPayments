package net.perfectdreams.perfectpayments.backend.utils.focusnfe

import kotlinx.serialization.Serializable

@Serializable
data class NFSeCancellationRequest(
    val justificativa: String
)