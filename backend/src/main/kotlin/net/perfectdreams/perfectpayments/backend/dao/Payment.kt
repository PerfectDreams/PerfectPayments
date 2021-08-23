package net.perfectdreams.perfectpayments.backend.dao

import net.perfectdreams.perfectpayments.backend.tables.Payments
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Payment(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Payment>(Payments)

    var gateway by Payments.gateway
    var status by Payments.status
    var callbackUrl by Payments.callbackUrl
    var createdAt by Payments.createdAt
    var paidAt by Payments.paidAt
    var referenceId by Payments.referenceId
    var amount by Payments.amount
    var title by Payments.title
    var currencyId by Payments.currencyId
}