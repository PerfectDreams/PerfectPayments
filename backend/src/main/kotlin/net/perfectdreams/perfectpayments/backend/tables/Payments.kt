package net.perfectdreams.perfectpayments.backend.tables

import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import org.jetbrains.exposed.dao.id.LongIdTable

object Payments : LongIdTable() {
    val gateway = enumeration("gateway", PaymentGateway::class)
    val status = enumeration("status", PaymentStatus::class)
    val referenceId = uuid("reference_id")
    val title = text("title")
    val amount = long("amount").index()
    val currencyId = text("currency_id")
    val callbackUrl = text("callback_url")
    val createdAt = long("created_at")
    val paidAt = long("paid_at").nullable()
}