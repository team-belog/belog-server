package org.com.belog.home.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.home.controller.cursor.TicketCursorCodec
import org.com.belog.home.service.result.TicketListResult
import org.com.belog.postlog.controller.dto.response.PostLogTicketResponse

@Schema(description = "생성한 티켓 목록 조회 결과")
data class TicketListResponse(
    @field:Schema(description = "생성한 티켓 목록")
    val items: List<PostLogTicketResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: TicketListResult): TicketListResponse =
            TicketListResponse(
                items = result.items.map(PostLogTicketResponse::from),
                nextCursor = result.nextCursor?.let(TicketCursorCodec::encode),
                hasNext = result.hasNext,
            )
    }
}
