package net.perfectdreams.perfectpayments.backend.utils.focusnfe.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NFSeCreateRequest(
    @SerialName("data_emissao")
    val dataEmissao: String,
    @SerialName("regime_especial_tributacao")
    val regimeEspecialTributacao: Int,
    @SerialName("optante_simples_nacional")
    val optanteSimplesNacional: Boolean,
    val prestador: Prestador,
    val tomador: Tomador?,
    val servico: Servico
) {
    @Serializable
    data class Prestador(
        val cnpj: String,
        @SerialName("inscricao_municipal")
        val inscricaoMunicipal: String,
        @SerialName("codigo_municipio")
        val codigoMunicipio: String
    )

    @Serializable
    data class Tomador(
        val cpf: String? = null,
        val cnpj: String? = null,
        @SerialName("razao_social")
        val razaoSocial: String,
        @SerialName("email")
        val email: String
    )

    @Serializable
    data class Servico(
        val aliquota: Double,
        val discriminacao: String,
        @SerialName("iss_retido")
        val issRetido: Boolean,
        @SerialName("item_lista_servico")
        val itemListaServico: String,
        @SerialName("codigo_tributario_municipio")
        val codigoTributarioMunicipio: String,
        @SerialName("valor_servicos")
        val valorServicos: Double,
        @SerialName("codigo_cnae")
        val codigoCnae: String,
        @SerialName("percentual_total_tributos")
        val percentualTotalTributos: Double,
        @SerialName("fonte_total_tributos")
        val fonteTotalTributos: String,
    )
}