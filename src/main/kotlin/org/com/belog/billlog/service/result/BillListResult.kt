package org.com.belog.billlog.service.result

import java.time.LocalDate

data class BillListResult(
    val days: List<BillDayResult>,
    val nextCursorDate: LocalDate?,
    val hasNext: Boolean,
)

data class BillDayResult(
    val dayNumber: Int,
    val paymentDate: LocalDate,
    val dailyTotalAmount: Long,
    val bills: List<BillListItemResult>,
)

data class BillListItemResult(
    val billId: Long,
    val title: String,
    val payerNickname: String,
    val totalAmount: Long,
)
