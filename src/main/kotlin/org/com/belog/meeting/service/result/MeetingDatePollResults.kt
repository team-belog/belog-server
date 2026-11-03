package org.com.belog.meeting.service.result

import java.time.LocalDate

data class MeetingDatePollResults(
    val meetingId: Long,
    val totalParticipantCount: Int,
    val respondedParticipantCount: Int,
    val candidateDateResults: List<CandidateDatePollResult>,
)

data class DatePollMemberResult(
    val groupMemberId: Long,
    val nickname: String,
)

data class CandidateDatePollResult(
    val candidateDateRangeId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rank: Int,
    val availableCount: Int,
    val availableMembers: List<DatePollMemberResult>,
    val unavailableMembers: List<DatePollMemberResult>,
)
