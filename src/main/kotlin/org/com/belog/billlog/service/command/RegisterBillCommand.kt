package org.com.belog.billlog.service.command

import org.com.belog.billlog.domain.BillSplitType

data class RegisterBillCommand(
    val meetingId: Long,
    val creatorUserId: Long,
    val title: String,
    val payerMemberId: Long,
    val totalAmount: Long,
    val splitType: BillSplitType,
    val items: List<BillItemCommand>,
    val shares: List<BillShareCommand>,
)

data class BillItemCommand(
    val name: String,
    val amount: Long,
)

data class BillShareCommand(
    val participantMemberId: Long,
    val amount: Long,
)
