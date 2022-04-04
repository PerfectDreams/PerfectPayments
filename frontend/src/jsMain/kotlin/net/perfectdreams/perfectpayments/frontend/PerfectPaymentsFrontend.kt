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
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentMethodSelection
import net.perfectdreams.perfectpayments.frontend.components.DataCollected
import net.perfectdreams.perfectpayments.frontend.components.NotaFiscalDataCollectRequest
import net.perfectdreams.perfectpayments.frontend.components.NotaFiscalRequest
import net.perfectdreams.perfectpayments.frontend.components.ParentalWarning
import net.perfectdreams.perfectpayments.frontend.components.PicPayDataCollectRequest
import net.perfectdreams.perfectpayments.frontend.components.SelectUserFacingPaymentMethod
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeys
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

    private var screenState = mutableStateOf<Screen>(Screen.SelectGateway)
    private var delegatedScreenState by screenState
    private var previousScreenState: Screen? = null

    var partialPaymentData by mutableStateOf<ClientSidePartialPayment?>(null)
    var i18nContext by mutableStateOf<I18nContext?>(null)
    var availableGateways by mutableStateOf<List<PaymentGateway>?>(null)

    fun start() {
        println("[DEBUG] Starting PerfectPayments...") // TODO: Remove this

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

        // Handle back button, this is kinda hacky but it works
        window.onpopstate = {
            val previousScreenState = previousScreenState
            if (previousScreenState != null) {
                this@PerfectPaymentsFrontend.previousScreenState = delegatedScreenState
                delegatedScreenState = previousScreenState
            }
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
                        SelectUserFacingPaymentMethod(this@PerfectPaymentsFrontend, i18nContext, partialPaymentData, UserFacingPaymentMethodSelection.selections)
                    }

                    is Screen.SelectSubGateway -> {
                        SelectUserFacingPaymentMethod(this@PerfectPaymentsFrontend, i18nContext, partialPaymentData, screen.group.methods)
                    }

                    is Screen.ParentalWarningRequest -> {
                        ParentalWarning(this@PerfectPaymentsFrontend, i18nContext, partialPaymentData, screen)
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
                        val footerText = i18nContext.language.textBundle.strings[I18nKeys.Footer.Text.key]!!
                        val splitted = footerText.split("{clickHere}")

                        Text(splitted[0])
                        A(href = "https://loritta.website/support") {
                            Text(i18nContext.get(I18nKeys.Footer.ClickHere))
                        }
                        Text(splitted[1])
                    }
                }
            }
        }
    }

    fun switch(screen: Screen) {
        // yes this is a hack because I don't wanna implement custom page path, how could you tell?
        window.history.pushState(window.location.pathname, "", window.location.pathname)
        previousScreenState = delegatedScreenState
        delegatedScreenState = screen
    }
}