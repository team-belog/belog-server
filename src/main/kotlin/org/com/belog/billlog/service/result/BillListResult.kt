package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.CurrencyAmount
import java.math.BigDecimal
import java.time.LocalDate

data class BillListResult(
    val days: List<BillDayResult>,
    val nextCursorDate: LocalDate?,
    val hasNext: Boolean,
)

data class BillDayResult(
    val dayNumber: Int,
    val paymentDate: LocalDate,
    val dailyTotalAmounts: List<CurrencyAmount>,
    val bills: List<BillListItemResult>,
)

data class BillListItemResult(
    val billId: Long,
    val title: String,
    val payerNickname: String,
    val currency: BillCurrency,
    val totalAmount: BigDecimal,
)
