package org.com.belog.postlog.service.result

import java.time.LocalDate

data class PostLogTicketCalendarResult(
    val year: Int,
    val month: Int,
    val memoryCount: Int,
    val tickets: List<PostLogTicketCalendarItemResult>,
)

data class PostLogTicketCalendarItemResult(
    val postLogId: Long,
    val meetingEndDate: LocalDate,
    val thumbnailUrl: String?,
)
