package org.com.belog.meeting.domain

import java.time.LocalDate

data class MeetingDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "종료일은 시작일보다 빠를 수 없습니다."
        }
    }
}
