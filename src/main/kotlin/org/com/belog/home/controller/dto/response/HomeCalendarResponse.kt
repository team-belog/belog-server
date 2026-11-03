package org.com.belog.home.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.home.service.result.HomeCalendarMeetingResult
import org.com.belog.home.service.result.HomeCalendarResult
import java.time.LocalDate
import java.time.YearMonth

@Schema(description = "홈 월간 달력 조회 결과")
data class HomeCalendarResponse(
    @field:Schema(description = "조회 연월", example = "2026-08")
    val yearMonth: YearMonth,
    @field:Schema(description = "서비스 기준 오늘 날짜", example = "2026-08-18")
    val today: LocalDate,
    @field:Schema(description = "읽지 않은 알림 존재 여부", example = "true")
    val hasUnreadNotification: Boolean,
    @field:Schema(description = "조회 월과 일정이 겹치는 확정 만남 목록")
    val meetings: List<HomeCalendarMeetingResponse>,
) {
    companion object {
        fun from(result: HomeCalendarResult): HomeCalendarResponse =
            HomeCalendarResponse(
                yearMonth = result.yearMonth,
                today = result.today,
                hasUnreadNotification = result.hasUnreadNotification,
                meetings = result.meetings.map(HomeCalendarMeetingResponse::from),
            )
    }
}

@Schema(description = "홈 달력 만남 일정")
data class HomeCalendarMeetingResponse(
    @field:Schema(description = "만남 ID", example = "11")
    val meetingId: Long,
    @field:Schema(description = "만남 시작일", example = "2026-08-17")
    val startDate: LocalDate,
    @field:Schema(description = "만남 종료일", example = "2026-08-18")
    val endDate: LocalDate,
) {
    companion object {
        fun from(result: HomeCalendarMeetingResult): HomeCalendarMeetingResponse =
            HomeCalendarMeetingResponse(
                meetingId = result.meetingId,
                startDate = result.startDate,
                endDate = result.endDate,
            )
    }
}
