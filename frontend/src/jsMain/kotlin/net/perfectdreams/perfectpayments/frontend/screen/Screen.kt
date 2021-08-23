package net.perfectdreams.perfectpayments.frontend.screen

import net.perfectdreams.perfectpayments.common.data.PersonalData
import net.perfectdreams.perfectpayments.common.data.PicPayPersonalData
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend

sealed class Screen {
    object SelectGateway : Screen()
    class NotaFiscalRequest(val gateway: PaymentGateway) : Screen()
    class NotaFiscalDataCollectRequest(val gateway: PaymentGateway) : Screen()
    class PicPayDataCollectRequest(val gateway: PaymentGateway, val personalData: PersonalData? = null) : Screen()
    class DataCollected(
        val gateway: PaymentGateway,
        val personalData: PersonalData? = null,
        val picPayPersonalData: PicPayPersonalData? = null
    ) : Screen()
}