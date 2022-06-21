package net.perfectdreams.perfectpayments.backend.utils.focusnfe.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(NFSeCreateResponse.Serializer::class)
sealed class NFSeCreateResponse {
    @Serializable
    class Success(
        @SerialName("cnpj_prestador")
        val cnpjPrestador: String,
        val ref: String,
        val status: String
    ) : NFSeCreateResponse()

    @Serializable
    sealed class Error : NFSeCreateResponse() {
        abstract val codigo: String
        abstract val mensagem: String
    }

    @Serializable
    data class AlreadyBeingProcessed(
        override val codigo: String,
        override val mensagem: String
    ) : Error()

    @Serializable
    data class RateLimited(
        override val codigo: String,
        override val mensagem: String
    ) : Error()

    @Serializable
    data class UnknownError(
        override val codigo: String,
        override val mensagem: String
    ) : Error()

    internal object Serializer : JsonContentPolymorphicSerializer<NFSeCreateResponse>(NFSeCreateResponse::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out NFSeCreateResponse> {
            val responseCode = element.jsonObject["codigo"]

            return if (responseCode != null) {
                when (responseCode.jsonPrimitive.content) {
                    "limite_excedido" -> RateLimited.serializer()
                    "em_processamento" -> AlreadyBeingProcessed.serializer()
                    else -> UnknownError.serializer()
                }
            } else {
                Success.serializer()
            }
        }
    }
}