package net.perfectdreams.perfectpayments.backend.processors.creators

import com.mercadopago.client.preference.PreferenceClient
import com.mercadopago.client.preference.PreferenceItemRequest
import com.mercadopago.client.preference.PreferenceRequest
import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import net.perfectdreams.perfectpayments.backend.utils.TextUtils
import java.math.BigDecimal

class MercadoPagoPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    val client = PreferenceClient()

    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedMercadoPagoPaymentInfo {
        val itemRequest =
            PreferenceItemRequest.builder()
                .title(TextUtils.cleanTitle(partialPayment.title))
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(BigDecimal(partialPayment.amount / 100.0))
                .build()
        val items: MutableList<PreferenceItemRequest> = ArrayList()
        items.add(itemRequest)
        val preferenceRequest = PreferenceRequest.builder()
            .externalReference(partialPayment.externalReference.format(paymentId))
            // No need for this, the URL is configured in the MercadoPago's dashboard (also, this setups webhooks AND IPNs, if we only want webhooks, we need
            // to set "?source_news=webhook")
            // .notificationUrl(m.gateway.mercadoPago.callbackUrl)
            .items(items)
            .build()
        val preference = client.create(preferenceRequest)

        return CreatedMercadoPagoPaymentInfo(
            preference.id,
            preference.initPoint
        )
    }
}