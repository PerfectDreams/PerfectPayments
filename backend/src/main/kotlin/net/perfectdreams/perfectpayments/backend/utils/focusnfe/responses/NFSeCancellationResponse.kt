package net.perfectdreams.perfectpayments.backend.utils.focusnfe.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = NFSeCancellationResponse.Serializer::class)
sealed class NFSeCancellationResponse {
    @Serializable
    data class Success(
        val status: String
    ) : NFSeCancellationResponse()

    @Serializable
    data class ErrorWhileCancelling(
        val status: String,
        val erros: List<NFSeError>? = null
    ) : NFSeCancellationResponse()

    @Serializable
    data class NFSeError(
        val codigo: String,
        val mensagem: String,
        val correcao: String? = null
    )

    @Serializable
    sealed class GenericError : NFSeCancellationResponse() {
        abstract val codigo: String
        abstract val mensagem: String
    }

    @Serializable
    sealed class UnknownError(
        override val codigo: String,
        override val mensagem: String
    ) : GenericError()

    internal object Serializer : JsonContentPolymorphicSerializer<NFSeCancellationResponse>(NFSeCancellationResponse::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out NFSeCancellationResponse> {
            val responseCode = element.jsonObject["codigo"]

            return if (responseCode != null) {
                when (responseCode.jsonPrimitive.content) {
                    else -> GenericError.serializer()
                }
            } else {
                val status = element.jsonObject["status"]!!.jsonPrimitive.content

                return when (status) {
                    "cancelada" -> Success.serializer()
                    "erro_cancelamento" -> ErrorWhileCancelling.serializer()
                    else -> error("I don't know how to handle $status!")
                }
            }
        }
    }
}