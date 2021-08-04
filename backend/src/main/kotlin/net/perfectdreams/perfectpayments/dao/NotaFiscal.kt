package net.perfectdreams.perfectpayments.dao

import net.perfectdreams.perfectpayments.tables.NotaFiscais
import net.perfectdreams.perfectpayments.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.tables.Payments
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class NotaFiscal(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NotaFiscal>(NotaFiscais)

    var payment by Payment referencedOn NotaFiscais.payment
    var personalInfo by PaymentPersonalInfo optionalReferencedOn NotaFiscais.personalInfo
    var status by NotaFiscais.status
}