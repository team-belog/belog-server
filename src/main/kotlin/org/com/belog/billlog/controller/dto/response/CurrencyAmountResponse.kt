package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.CurrencyAmount
import java.math.BigDecimal

@Schema(description = "화폐 단위별 금액")
data class CurrencyAmountResponse(
    @field:Schema(description = "화폐 단위", example = "KRW", allowableValues = ["KRW", "USD", "JPY", "EUR"])
    val currency: BillCurrency,
    @field:Schema(description = "금액. KRW와 JPY는 정수, USD와 EUR는 소수점 둘째 자리까지 표현합니다.", example = "11000")
    val amount: BigDecimal,
) {
    companion object {
        fun from(currencyAmount: CurrencyAmount): CurrencyAmountResponse =
            CurrencyAmountResponse(
                currency = currencyAmount.currency,
                amount = currencyAmount.amount,
            )
    }
}
