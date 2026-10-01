package org.com.belog.home.service.result

import java.time.LocalDate
import java.time.YearMonth

data class HomeCalendarResult(
    val yearMonth: YearMonth,
    val today: LocalDate,
    val meetings: List<HomeCalendarMeetingResult>,
)

data class HomeCalendarMeetingResult(
    val meetingId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
