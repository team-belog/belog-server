package org.com.belog.home.service.query

import java.time.LocalDate

data class ActiveMeetingCursor(
    val type: ActiveMeetingCursorType,
    val startDate: LocalDate?,
    val meetingId: Long,
) {
    init {
        require(meetingId > 0) { "진행 중인 만남 목록 커서의 만남 ID는 양수여야 합니다." }
        require(
            (type == ActiveMeetingCursorType.SCHEDULING && startDate == null) ||
                (type == ActiveMeetingCursorType.CONFIRMED && startDate != null),
        ) { "진행 중인 만남 목록 커서의 상태와 시작일이 일치하지 않습니다." }
    }
}

enum class ActiveMeetingCursorType {
    SCHEDULING,
    CONFIRMED,
}
