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

sealed class Screen {
    object SelectGateway : Screen()
    class ParentalWarningRequest(val gateway: PaymentGateway) : Screen() {
        var countdown by mutableStateOf(7)

        // TODO: This is super hacky omg
        init {
            GlobalScope.launch {
                while (countdown != 0) {
                    delay(1_000)
                    countdown--
                }
            }
        }
    }

    class NotaFiscalRequest(val gateway: PaymentGateway) : Screen()
    class NotaFiscalDataCollectRequest(val gateway: PaymentGateway) : Screen()
    class PicPayDataCollectRequest(val gateway: PaymentGateway, val personalData: PersonalData? = null) : Screen()
    class DataCollected(
        val gateway: PaymentGateway,
        val personalData: PersonalData? = null,
        val picPayPersonalData: PicPayPersonalData? = null
    ) : Screen()
}