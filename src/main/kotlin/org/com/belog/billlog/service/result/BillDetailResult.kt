package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.BillSplitType
import java.time.LocalDate

data class BillDetailResult(
    val billId: Long,
    val dayNumber: Int,
    val paymentDate: LocalDate,
    val title: String,
    val payerNickname: String,
    val settlementMethod: BillSplitType,
    val items: List<BillItemResult>,
    val totalAmount: Long,
    val shares: List<BillShareResult>,
)

data class BillItemResult(
    val name: String,
    val amount: Long,
)

data class BillShareResult(
    val meetingParticipantId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val amount: Long,
    val payer: Boolean,
)
