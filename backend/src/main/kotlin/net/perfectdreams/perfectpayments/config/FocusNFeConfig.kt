package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable

@Serializable
class FocusNFeConfig(
    val url: String,
    val token: String,
    val prestador: PrestadorConfig,
    val referencePrefix: String,
    val callbackAuthorization: String
) {
    @Serializable
    class PrestadorConfig(
        val cnpj: String,
        val inscricaoMunicipal: String,
        val codigoMunicipio: String
    )
}