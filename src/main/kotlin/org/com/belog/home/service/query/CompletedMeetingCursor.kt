package org.com.belog.home.service.query

import java.time.LocalDate

data class CompletedMeetingCursor(
    val endDate: LocalDate,
    val postLogId: Long,
) {
    init {
        require(postLogId > 0) { "종료된 만남 목록 커서의 Post-log ID는 양수여야 합니다." }
    }
}
