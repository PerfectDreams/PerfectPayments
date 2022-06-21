package net.perfectdreams.perfectpayments.backend.utils.focusnfe

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.config.FocusNFeConfig
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class FocusNFe(private val config: FocusNFeConfig) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val json = Json {
            ignoreUnknownKeys = true
        }
        private val http = HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                // Because FocusNFe is sometimes kinda finicky ngl
                // "Request timeout has expired [url=https://api.focusnfe.com.br/v2/nfse?ref=pp-prod-1507, request_timeout=1000 ms]"
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }

        /**
         * Characters allowed in an nota fiscal "reason" section
         */
        private val reasonFilterRegex = Regex("[^\\w0-9 ]")
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
            date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
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
                description.replace(reasonFilterRegex, ""),
                false,
                "2800", // Precisa verificar na tabela de 2021
                "0105", // Precisa retirar os pontos do número, de https://conube.com.br/blog/tabela-iss-sp/
                value,
                "6202300",
                config.percentualTotalTributos,
                "SEBRAE"
            )
        )

        val result = executeFocusNFeRequest("/v2/nfse?ref=$ref") {
            method = HttpMethod.Post

            setBody(
                TextContent(
                    Json.encodeToString(request),
                    ContentType.Application.Json
                )
            )
        }

        logger.info { "Successfully registered a nota fiscal for $ref!" }
    }

    suspend fun cancelNFSe(
        ref: String
    ): NFSeCancellationResponse {
        val request = NFSeCancellationRequest(ref)

        val result = executeFocusNFeRequest("/v2/nfse?ref=$ref") {
            method = HttpMethod.Delete

            setBody(
                TextContent(
                    Json.encodeToString(request),
                    ContentType.Application.Json
                )
            )
        }

        return json.decodeFromString(result.bodyAsText(Charsets.UTF_8))
    }

    private suspend fun executeFocusNFeRequest(path: String, httpRequestBuilder: HttpRequestBuilder.() -> (Unit)): HttpResponse {
        logger.info { "Executing FocusNFe request to $path" }

        val result = http.request("${config.url.removeSuffix("/")}$path") {
            val auth = Base64.getEncoder().encodeToString("${config.token}:".toByteArray(Charsets.UTF_8)) // The password is always empty
            header("Authorization", "Basic $auth")

            httpRequestBuilder.invoke(this)
        }

        logger.info { "FocusNFe request result: ${result.status}" }

        if (result.status == HttpStatusCode.TooManyRequests) {
            val ratelimitRetryAfter = result.headers["Rate-Limit-Reset"]
            val rlRetryAfter = ratelimitRetryAfter?.toLong()
            if (rlRetryAfter != null) {
                logger.warn { "Request is rate limited! Waiting ${rlRetryAfter}s before retrying..." }
                delay(rlRetryAfter * 1000)
                return executeFocusNFeRequest(path, httpRequestBuilder)
            }
        }

        // Unprocessable Entity =
        // 422 	nfe_nao_autorizada 	Foi feita alguma operação com a nota que só é aplicável se ela estiver autorizada (por exemplo a ação de cancelamento)
        // 422 	nfe_autorizada 	Foi solicitado o processamento de uma nota já autorizada
        // 422 	em_processamento 	Foi solicitado o processamento de uma nota que já está em processamento
        // TODO: Parse and respect these
        // {
        //  "codigo": "nfe_autorizada",
        //  "mensagem": "Nota fiscal já autorizada"
        // }
        // {
        //  "codigo": "limite_excedido",
        //  "mensagem": "Número máximo de requisições por minuto (100) excedido. Tente novamente em 43 segundos"
        // }
        // result.status.isSuccess() || result.status == HttpStatusCode.UnprocessableEntity

        return result
    }
}