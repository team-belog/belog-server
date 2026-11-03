package org.com.belog.meeting.service.result

import java.time.LocalDate

data class PastMeetingListResult(
    val items: List<PastMeetingResult>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class PastMeetingResult(
    val meetingId: Long,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
