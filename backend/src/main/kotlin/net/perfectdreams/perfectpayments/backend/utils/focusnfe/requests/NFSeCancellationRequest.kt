package net.perfectdreams.perfectpayments.backend.utils.focusnfe.requests

import kotlinx.serialization.Serializable

@Serializable
data class NFSeCancellationRequest(
    val justificativa: String
)