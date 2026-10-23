package org.com.belog.postlog.service.result

import org.com.belog.billlog.domain.CurrencyAmount
import java.time.LocalDate

data class PostLogSummaryResult(
    val ticketId: Long?,
    val meetingId: Long,
    val meetingName: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val location: String?,
    val memory: String?,
    val totalAmounts: List<CurrencyAmount>,
    val completedParticipantCount: Long,
    val participants: List<PostLogParticipantResult>,
    val ticketCreated: Boolean,
)

data class PostLogParticipantResult(
    val groupMemberId: Long,
    val nickname: String,
    val meetingCreator: Boolean,
)
