package org.com.belog.billlog.domain

import java.math.BigDecimal

object BillAmount {
    const val PRECISION = 19
    const val SCALE = 2
    const val MAX_INTEGER_DIGITS = PRECISION - SCALE

    fun isStorable(amount: BigDecimal): Boolean = amount.precision() - amount.scale() <= MAX_INTEGER_DIGITS
}

data class CurrencyAmount(
    val currency: BillCurrency,
    val amount: BigDecimal,
) {
    companion object {
        fun of(
            currency: BillCurrency,
            amount: BigDecimal,
        ): CurrencyAmount = CurrencyAmount(currency = currency, amount = currency.normalize(amount))

        fun sumByCurrency(amounts: Iterable<CurrencyAmount>): List<CurrencyAmount> =
            amounts
                .groupBy { currencyAmount -> currencyAmount.currency }
                .map { (currency, currencyAmounts) ->
                    of(currency, currencyAmounts.fold(BigDecimal.ZERO) { total, currencyAmount -> total.add(currencyAmount.amount) })
                }.sortedBy { currencyAmount -> currencyAmount.currency }
    }
}
