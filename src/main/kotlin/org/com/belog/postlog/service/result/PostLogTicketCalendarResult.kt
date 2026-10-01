package org.com.belog.postlog.service.result

import java.time.LocalDate
import java.time.YearMonth

data class PostLogTicketCalendarResult(
    val yearMonth: YearMonth,
    val today: LocalDate,
    val memoryCount: Int,
    val tickets: List<PostLogTicketCalendarItemResult>,
)

data class PostLogTicketCalendarItemResult(
    val postLogId: Long,
    val meetingEndDate: LocalDate,
    val thumbnailUrl: String?,
)
