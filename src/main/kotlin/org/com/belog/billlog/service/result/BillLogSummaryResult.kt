package org.com.belog.billlog.service.result

data class BillLogSummaryResult(
    val totalSpentAmount: Long,
    val completedParticipantCount: Long,
    val pendingParticipantCount: Long,
)
