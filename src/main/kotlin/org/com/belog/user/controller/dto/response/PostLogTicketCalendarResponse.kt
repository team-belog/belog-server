package org.com.belog.user.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogTicketCalendarItemResult
import org.com.belog.postlog.service.result.PostLogTicketCalendarResult
import java.time.LocalDate

@Schema(description = "마이페이지 월별 티켓 달력")
data class PostLogTicketCalendarResponse(
    @field:Schema(description = "조회 연도", example = "2026")
    val year: Int,
    @field:Schema(description = "조회 월", example = "8")
    val month: Int,
    @field:Schema(description = "조회 월에 표시되는 서로 다른 티켓 수", example = "2")
    val memoryCount: Int,
    @field:Schema(description = "만남 종료일 기준 티켓 목록")
    val tickets: List<PostLogTicketCalendarItemResponse>,
) {
    companion object {
        fun from(result: PostLogTicketCalendarResult): PostLogTicketCalendarResponse =
            PostLogTicketCalendarResponse(
                year = result.year,
                month = result.month,
                memoryCount = result.memoryCount,
                tickets = result.tickets.map(PostLogTicketCalendarItemResponse::from),
            )
    }
}

@Schema(description = "달력에 표시할 Post-log 티켓")
data class PostLogTicketCalendarItemResponse(
    @field:Schema(description = "개인 Post-log 티켓 ID", example = "31")
    val postLogId: Long,
    @field:Schema(description = "달력 표시 날짜인 만남 종료일", example = "2026-08-12")
    val meetingEndDate: LocalDate,
    @field:Schema(description = "대표 사진 조회 URL. 등록된 사진이 없으면 null", nullable = true)
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(result: PostLogTicketCalendarItemResult): PostLogTicketCalendarItemResponse =
            PostLogTicketCalendarItemResponse(
                postLogId = result.postLogId,
                meetingEndDate = result.meetingEndDate,
                thumbnailUrl = result.thumbnailUrl,
            )
    }
}
