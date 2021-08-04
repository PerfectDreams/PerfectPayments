package net.perfectdreams.perfectpayments.frontend

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.i18nhelper.formatters.IntlMFFormatter
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.common.data.PartialPaymentURL
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.frontend.components.DataCollected
import net.perfectdreams.perfectpayments.frontend.components.NotaFiscalDataCollectRequest
import net.perfectdreams.perfectpayments.frontend.components.NotaFiscalRequest
import net.perfectdreams.perfectpayments.frontend.components.PicPayDataCollectRequest
import net.perfectdreams.perfectpayments.frontend.components.SelectGateway
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.TranslationKeys
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody

class PerfectPaymentsFrontend {
    val http = HttpClient {
        expectSuccess = false
    }

    var screenState = mutableStateOf<Screen>(Screen.SelectGateway)
    var delegatedScreenState by screenState

    var partialPaymentData by mutableStateOf<ClientSidePartialPayment?>(null)
    var i18nContext by mutableStateOf<I18nContext?>(null)
    var availableGateways by mutableStateOf<List<PaymentGateway>?>(null)

    fun start() {
        val partialPaymentURL = try {
            PartialPaymentURL.fromString(window.location.pathname)
        } catch (e: IllegalArgumentException) {
            // If the parse fails, just return, because maybe it is just a "normal" page
            return
        }
        val partialPaymentId = partialPaymentURL.partialPaymentId

        GlobalScope.launch {
            val result = http.get<HttpResponse>("${window.location.origin}/api/v1/partial-payments/$partialPaymentId") {}

            if (result.status == HttpStatusCode.NotFound) {
                // Whoops, partial payment does not exist!
                window.location.replace("/missing")
                return@launch
            }

            partialPaymentData = Json.decodeFromString<ClientSidePartialPayment>(result.readText(Charsets.UTF_8))
        }

        GlobalScope.launch {
            val result = http.get<String>("${window.location.origin}/api/v1/strings") {}

            i18nContext = I18nContext(
                IntlMFFormatter(),
                Json.decodeFromString(result)
            )
        }

        GlobalScope.launch {
            val result = http.get<String>("${window.location.origin}/api/v1/gateways") {}

            availableGateways = Json.decodeFromString(result)
        }

        renderComposableInBody {
            val partialPaymentData = partialPaymentData
            val i18nContext = i18nContext

            if (partialPaymentData == null) {
                H1 {
                    Text("Carregando Payment Data...")
                }
            } else if (i18nContext == null) {
                H1 {
                    Text("Carregando Strings...")
                }
            } else if (availableGateways == null) {
                H1 {
                    Text("Carregando gateways disponíveis...")
                }
            } else {
                val rememberedScreenState by remember { screenState }

                when (val screen = rememberedScreenState) {
                    is Screen.SelectGateway -> {
                        SelectGateway(this@PerfectPaymentsFrontend, i18nContext, partialPaymentData)
                    }

                    is Screen.NotaFiscalRequest -> {
                        NotaFiscalRequest(this@PerfectPaymentsFrontend, i18nContext, partialPaymentData, screen)
                    }

                    is Screen.NotaFiscalDataCollectRequest -> {
                        NotaFiscalDataCollectRequest(
                            this@PerfectPaymentsFrontend,
                            i18nContext,
                            partialPaymentData,
                            screen
                        )
                    }

                    is Screen.PicPayDataCollectRequest -> {
                        PicPayDataCollectRequest(
                            this@PerfectPaymentsFrontend,
                            i18nContext,
                            partialPaymentData,
                            screen
                        )
                    }

                    is Screen.DataCollected -> {
                        DataCollected(this@PerfectPaymentsFrontend, i18nContext, partialPaymentId, screen)
                    }
                }

                Footer(attrs = { style { textAlign("center") } }) {
                    P {
                        // TODO: Fix this workaround when the compiler bug is fixed
                        // Workaround for now
                        val footerText = i18nContext.language.textBundle.strings[TranslationKeys.Footer.Text.key]!!
                        val splitted = footerText.split("{clickHere}")

                        Text(splitted[0])
                        A(href = "https://loritta.website/support") {
                            Text(i18nContext.get(TranslationKeys.Footer.ClickHere))
                        }
                        Text(splitted[1])
                    }
                }
            }
        }
    }
}