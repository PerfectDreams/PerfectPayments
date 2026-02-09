package net.perfectdreams.perfectpayments.backend.tables

import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Payments : LongIdTable() {
    val gateway = enumeration("gateway", PaymentGateway::class)
    val status = enumeration("status", PaymentStatus::class)
    val referenceId = uuid("reference_id")
    val title = text("title")
    val amount = long("amount").index()
    val currencyId = text("currency_id")
    val callbackUrl = text("callback_url")
    val createdAt = timestamp("created_at")
    val paidAt = timestamp("paid_at").nullable()
    val externalReferenceFormat = text("external_reference").nullable()
    val netReceivedAmount = long("net_received_amount").nullable()
}