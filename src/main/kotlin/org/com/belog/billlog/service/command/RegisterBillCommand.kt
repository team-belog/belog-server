package org.com.belog.billlog.service.command

import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.BillSplitType
import java.math.BigDecimal

data class RegisterBillCommand(
    val meetingId: Long,
    val creatorUserId: Long,
    val title: String,
    val payerMemberId: Long,
    val currency: BillCurrency,
    val totalAmount: BigDecimal,
    val splitType: BillSplitType,
    val items: List<BillItemCommand>,
    val shares: List<BillShareCommand>,
)

data class BillItemCommand(
    val name: String,
    val amount: BigDecimal,
)

data class BillShareCommand(
    val participantMemberId: Long,
    val amount: BigDecimal,
)
