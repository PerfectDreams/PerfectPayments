package net.perfectdreams.perfectpayments.backend.dao

import net.perfectdreams.perfectpayments.backend.tables.PaymentPersonalInfos
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PaymentPersonalInfo(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PaymentPersonalInfo>(PaymentPersonalInfos)

    var payment by Payment referencedOn  PaymentPersonalInfos.payment
    var socialNumber by PaymentPersonalInfos.socialNumber
    var name by PaymentPersonalInfos.name
    var email by PaymentPersonalInfos.email
}