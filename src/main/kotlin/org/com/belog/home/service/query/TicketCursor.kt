package org.com.belog.home.service.query

import java.time.LocalDate

data class TicketCursor(
    val endDate: LocalDate,
    val ticketId: Long,
) {
    init {
        require(ticketId > 0) { "티켓 목록 커서의 티켓 ID는 양수여야 합니다." }
    }
}
