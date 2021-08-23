package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.CPF
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.common.data.Email
import net.perfectdreams.perfectpayments.common.data.LegalName
import net.perfectdreams.perfectpayments.common.data.PersonalData
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.components.input.CPFInput
import net.perfectdreams.perfectpayments.frontend.components.input.EmailInput
import net.perfectdreams.perfectpayments.frontend.components.input.LegalNameInput
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Hr

@Composable
fun NotaFiscalDataCollectRequest(m: PerfectPaymentsFrontend, i18nContext: I18nContext, partialPaymentData: ClientSidePartialPayment, screen: Screen.NotaFiscalDataCollectRequest) {
    PaymentHeader(i18nContext, i18nContext.get(I18nKeys.TaxInvoice.FillYourPersonalData), partialPaymentData)

    Div({ id("wrapper") }) {
        var name by remember { mutableStateOf<LegalName?>(null) }
        var email by remember { mutableStateOf<Email?>(null) }
        var document by remember { mutableStateOf<CPF?>(null) }

        LegalNameInput(i18nContext) {
            name = it
        }

        EmailInput(i18nContext) {
            email = it
        }

        CPFInput(i18nContext) {
            document = it
        }

        Hr {}

        val everythingIsFilled = document != null && name != null && email != null

        Button(attrs = {
            classes("button", "primary")
            style { width(100.percent) }

            onClick {
                if (everythingIsFilled)
                    if (screen.gateway == PaymentGateway.PICPAY) {
                        m.delegatedScreenState = Screen.PicPayDataCollectRequest(
                            screen.gateway,
                            PersonalData(
                                document!!,
                                name!!,
                                email!!
                            )
                        )
                    } else {
                        m.delegatedScreenState = Screen.DataCollected(
                            screen.gateway,
                            PersonalData(
                                document!!,
                                name!!,
                                email!!
                            )
                        )
                    }
            }

            if (!everythingIsFilled)
                disabled()
        }) {
            TranslatedText(i18nContext, I18nKeysData.ContinueToPayment)
        }
    }
}