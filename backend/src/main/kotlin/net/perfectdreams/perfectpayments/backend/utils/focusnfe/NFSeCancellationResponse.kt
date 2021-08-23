package net.perfectdreams.perfectpayments.backend.utils.focusnfe

import kotlinx.serialization.Serializable

@Serializable
data class NFSeCancellationResponse(
    val status: String,
    val erros: List<NFSeError>? = null
) {
    @Serializable
    data class NFSeError(
        val codigo: String,
        val mensagem: String,
        val correcao: String? = null
    )
}