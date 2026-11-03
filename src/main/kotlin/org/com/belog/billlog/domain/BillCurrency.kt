package org.com.belog.billlog.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class BillCurrency(
    val fractionDigits: Int,
) {
    KRW(0),
    USD(2),
    JPY(0),
    EUR(2),
    ;

    fun supports(amount: BigDecimal): Boolean = amount.stripTrailingZeros().scale() <= fractionDigits

    fun normalize(amount: BigDecimal): BigDecimal = amount.setScale(fractionDigits, RoundingMode.UNNECESSARY)

    companion object {
        const val COLUMN_LENGTH = 3
    }
}
