package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.BillSplitType

data class RegisteredBill(
    val billId: Long,
    val meetingId: Long,
    val totalAmount: Long,
    val splitType: BillSplitType,
    val itemCount: Int,
    val shareCount: Int,
    val settlementRequestCount: Int,
)
