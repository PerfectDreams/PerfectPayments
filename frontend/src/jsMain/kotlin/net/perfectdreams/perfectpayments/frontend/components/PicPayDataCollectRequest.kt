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
import net.perfectdreams.perfectpayments.common.data.FirstName
import net.perfectdreams.perfectpayments.common.data.LastName
import net.perfectdreams.perfectpayments.common.data.PhoneNumber
import net.perfectdreams.perfectpayments.common.data.PicPayPersonalData
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.components.input.CPFInput
import net.perfectdreams.perfectpayments.frontend.components.input.EmailInput
import net.perfectdreams.perfectpayments.frontend.components.input.FirstNameInput
import net.perfectdreams.perfectpayments.frontend.components.input.LastNameInput
import net.perfectdreams.perfectpayments.frontend.components.input.PhoneInput
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.P

@Composable
fun PicPayDataCollectRequest(
    m: PerfectPaymentsFrontend,
    i18nContext: I18nContext,
    partialPaymentData: ClientSidePartialPayment,
    screen: Screen.PicPayDataCollectRequest
) {
    PaymentHeader(i18nContext, i18nContext.get(I18nKeys.TaxInvoice.FillYourPersonalData), partialPaymentData)

    Div({ id("wrapper") }) {
        var firstName by remember { mutableStateOf<FirstName?>(null) }
        var lastName by remember { mutableStateOf<LastName?>(null) }
        var email by remember { mutableStateOf<Email?>(null) }
        var document by remember { mutableStateOf<CPF?>(null) }
        var phoneNumber by remember { mutableStateOf<PhoneNumber?>(null) }

        FirstNameInput(i18nContext) {
            firstName = it
        }

        LastNameInput(i18nContext) {
            lastName = it
        }

        EmailInput(i18nContext) {
            email = it
        }

        CPFInput(i18nContext) {
            document = it
        }

        PhoneInput(i18nContext) {
            phoneNumber = it
        }

        P {
            TranslatedText(i18nContext, I18nKeysData.Gateways.Picpay.DataWarning)
        }

        Hr {}

        val everythingIsFilled = (document != null && firstName != null && lastName != null && email != null && phoneNumber != null)
        Button(attrs = {
            classes("button", "primary")
            style { width(100.percent) }

            onClick {
                if (everythingIsFilled)
                    m.delegatedScreenState = Screen.DataCollected(
                        screen.gateway,
                        screen.personalData,
                        PicPayPersonalData(
                            document!!,
                            firstName!!,
                            lastName!!,
                            email!!,
                            phoneNumber!!
                        )
                    )
            }

            if (!everythingIsFilled)
                disabled()
        }) {
            TranslatedText(i18nContext, I18nKeysData.ContinueToPayment)
        }
    }
}