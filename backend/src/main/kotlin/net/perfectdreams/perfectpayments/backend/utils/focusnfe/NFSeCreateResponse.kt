package net.perfectdreams.perfectpayments.backend.utils.focusnfe

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
        abstract val code: String
        abstract val mensagem: String
    }

    @Serializable
    class AlreadyBeingProcessed(
        @SerialName("codigo")
        override val code: String,
        override val mensagem: String
    ) : Error()

    @Serializable
    class RateLimited(
        @SerialName("codigo")
        override val code: String,
        override val mensagem: String
    ) : Error()

    internal object Serializer : JsonContentPolymorphicSerializer<NFSeCreateResponse>(NFSeCreateResponse::class) {
        override fun selectDeserializer(element: JsonElement): KSerializer<out NFSeCreateResponse> {
            val responseCode = element.jsonObject["codigo"]

            return if (responseCode != null) {
                when (responseCode.jsonPrimitive.content) {
                    "limite_excedido" -> RateLimited.serializer()
                    "em_processamento" -> AlreadyBeingProcessed.serializer()
                    else -> Error.serializer()
                }
            } else {
                Success.serializer()
            }
        }
    }
}