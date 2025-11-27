package net.perfectdreams.perfectpayments.frontend.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.perfectdreams.perfectpayments.common.data.PersonalData
import net.perfectdreams.perfectpayments.common.data.PicPayPersonalData
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentGroup

sealed class Screen {
    object SelectGateway : Screen()
    class SelectSubGateway(val group: UserFacingPaymentGroup) : Screen()
    class ParentalWarningRequest(val gateway: PaymentGateway) : Screen()

    class NotaFiscalRequest(val gateway: PaymentGateway) : Screen()
    class NotaFiscalDataCollectRequest(val gateway: PaymentGateway) : Screen()
    class PicPayDataCollectRequest(val gateway: PaymentGateway, val personalData: PersonalData? = null) : Screen()
    class DataCollected(
        val gateway: PaymentGateway,
        val personalData: PersonalData? = null,
        val picPayPersonalData: PicPayPersonalData? = null
    ) : Screen()
}