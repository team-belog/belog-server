package org.com.belog.home.service.query

import java.time.LocalDate

data class CompletedMeetingCursor(
    val endDate: LocalDate,
    val ticketId: Long,
) {
    init {
        require(ticketId > 0) { "종료된 만남 목록 커서의 티켓 ID는 양수여야 합니다." }
    }
}
