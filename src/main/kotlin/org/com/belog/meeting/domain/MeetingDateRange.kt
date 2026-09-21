package org.com.belog.meeting.domain

import java.time.LocalDate

data class MeetingDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
)
