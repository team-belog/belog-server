package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.BillSplitType
import java.math.BigDecimal
import java.time.LocalDate

data class BillDetailResult(
    val billId: Long,
    val dayNumber: Int,
    val paymentDate: LocalDate,
    val title: String,
    val payerNickname: String,
    val settlementMethod: BillSplitType,
    val currency: BillCurrency,
    val items: List<BillItemResult>,
    val totalAmount: BigDecimal,
    val shares: List<BillShareResult>,
)

data class BillItemResult(
    val name: String,
    val amount: BigDecimal,
)

data class BillShareResult(
    val meetingParticipantId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val amount: BigDecimal,
    val payer: Boolean,
)
