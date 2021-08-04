package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import io.ktor.client.request.*
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.common.data.FilledPartialPayment
import net.perfectdreams.perfectpayments.common.data.PaymentCreatedResponse
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun DataCollected(m: PerfectPaymentsFrontend, i18nContext: I18nContext, partialPaymentId: String, screen: Screen.DataCollected) {
    H1 {
        Text("Finalizando pagamento...")
    }

    GlobalScope.launch {
        val result = m.http.post<String>("${window.location.origin}/api/v1/partial-payments/$partialPaymentId/finish") {
            body = Json.encodeToString(
                FilledPartialPayment(
                    screen.gateway,
                    screen.personalData,
                    screen.picPayPersonalData
                )
            )
        }

        val fromJson = Json.decodeFromString<PaymentCreatedResponse>(result)
        window.location.replace(fromJson.url)
    }
}