package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.CurrencyAmount

data class BillLogSummaryResult(
    val totalSpentAmounts: List<CurrencyAmount>,
    val completedParticipantCount: Long,
    val pendingParticipantCount: Long,
)
