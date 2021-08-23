package net.perfectdreams.perfectpayments.backend.utils.focusnfe

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.config.FocusNFeConfig
import net.perfectdreams.perfectpayments.backend.utils.callbackWithBackoff
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FocusNFe(private val config: FocusNFeConfig) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    // We send NFSe because infoproducts are considered "services", this is confused as heck.
    //
    // Example: If you are selling a ebook, it is a service because you are offering the URL to download the ebook
    //
    // NFes also require the user's location, which is pain.
    suspend fun createNFSe(
        ref: String,
        date: ZonedDateTime,
        value: Double,
        description: String,
        tomador: NFSeCreateRequest.Tomador?
    ) {
        val request = NFSeCreateRequest(
            date.format(DateTimeFormatter.ISO_INSTANT),
            // 1: Microempresa municipal;
            // 2: Estimativa;
            // 3: Sociedade de profissionais;
            // 4: Cooperativa;
            // 5: MEI - Simples Nacional;
            // 6: ME EPP- Simples Nacional.
            regimeEspecialTributacao = 6,
            optanteSimplesNacional = true,
            prestador = NFSeCreateRequest.Prestador(
                config.prestador.cnpj,
                config.prestador.inscricaoMunicipal,
                config.prestador.codigoMunicipio
            ),
            tomador,
            servico = NFSeCreateRequest.Servico(
                0.0, // 0, pois o tributo é coletado pelo SEBRAE
                description,
                false,
                "2800", // Precisa verificar na tabela de 2021
                "0105", // Precisa retirar os pontos do número, de https://conube.com.br/blog/tabela-iss-sp/
                value,
                "6202300",
                0.155,
                "SEBRAE"
            )
        )

        val callbackWithBackoff = callbackWithBackoff(
            {
                val result = PerfectPayments.http.post<HttpResponse>("${config.url.removeSuffix("/")}/v2/nfse?ref=$ref") {
                    val auth = Base64.getEncoder().encodeToString("${config.token}:".toByteArray(Charsets.UTF_8)) // The password is always empty
                    header("Authorization", "Basic $auth")

                    body = TextContent(
                        Json.encodeToString(request),
                        ContentType.Application.Json
                    )
                }

                println(result.readText())

                result.status.isSuccess()
            },
            {
                logger.warn { "Something went wrong while trying to register a nota fiscal! Retrying again after ${it}ms"}
            }
        )
    }

    suspend fun cancelNFSe(
        ref: String
    ): NFSeCancellationResponse {
        val request = NFSeCancellationRequest(ref)

        val result = PerfectPayments.http.delete<HttpResponse>("${config.url.removeSuffix("/")}/v2/nfse?ref=$ref") {
            val auth = Base64.getEncoder().encodeToString("${config.token}:".toByteArray(Charsets.UTF_8)) // The password is always empty
            header("Authorization", "Basic $auth")

            body = TextContent(
                Json.encodeToString(request),
                ContentType.Application.Json
            )
        }

        return json.decodeFromString(result.readText(Charsets.UTF_8))
    }
}