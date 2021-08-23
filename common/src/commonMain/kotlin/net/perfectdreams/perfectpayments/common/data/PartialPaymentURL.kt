package net.perfectdreams.perfectpayments.common.data

import kotlin.jvm.JvmInline

@JvmInline
value class PartialPaymentURL private constructor(val partialPaymentId: String) {
    companion object {
        fun fromString(input: String): PartialPaymentURL {
            val split = input.split("/")

            if (3 > split.size)
                throw IllegalArgumentException("Invalid Partial Payment URL!")

            return PartialPaymentURL(split[2])
        }
    }
}