package org.com.belog.meeting.service.result

import org.com.belog.meeting.domain.MeetingStatus
import java.time.LocalDate

data class MeetingDatePollResult(
    val meetingId: Long,
    val status: MeetingStatus,
    val candidateDateRanges: List<CandidateDateRangeResult>,
)

data class CandidateDateRangeResult(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
