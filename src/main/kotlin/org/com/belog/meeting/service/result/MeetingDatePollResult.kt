package org.com.belog.meeting.service.result

import org.com.belog.meeting.domain.MeetingStatus
import java.time.Instant
import java.time.LocalDate

data class MeetingDatePollResult(
    val meetingId: Long,
    val status: MeetingStatus,
    val candidateDateRanges: List<CandidateDateRangeResult>,
    val myResponse: MyDatePollResponseResult,
)

data class CandidateDateRangeResult(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class MyDatePollResponseResult(
    val responded: Boolean,
    val respondedAt: Instant?,
    val selectedCandidateDateRangeIds: List<Long>,
)
