package org.com.belog.home.service.result

import org.com.belog.home.service.query.CompletedMeetingCursor
import java.time.LocalDate

data class CompletedMeetingListResult(
    val items: List<CompletedMeetingResult>,
    val nextCursor: CompletedMeetingCursor?,
    val hasNext: Boolean,
)

data class CompletedMeetingResult(
    val ticketId: Long,
    val name: String,
    val memory: String,
    val coverPhotoUrl: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val location: String?,
    val participantCount: Int,
    val participants: List<CompletedMeetingParticipantResult>,
)

data class CompletedMeetingParticipantResult(
    val groupMemberId: Long,
    val nickname: String,
)
