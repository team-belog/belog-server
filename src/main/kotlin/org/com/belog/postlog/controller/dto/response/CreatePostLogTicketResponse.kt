package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.domain.PostLog

@Schema(description = "Post-log 티켓 생성 결과")
data class CreatePostLogTicketResponse(
    @field:Schema(description = "Post-log ID", example = "11")
    val postLogId: Long,
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "티켓에 들어가는 추억 문구", example = "함께한 광주 여행을 오래 기억하자")
    val memory: String,
    @field:Schema(description = "티켓 생성 여부", example = "true")
    val ticketCreated: Boolean,
) {
    companion object {
        fun from(postLog: PostLog): CreatePostLogTicketResponse =
            CreatePostLogTicketResponse(
                postLogId = checkNotNull(postLog.id) { "생성된 Post-log의 ID가 없습니다." },
                meetingId = checkNotNull(postLog.meeting.id) { "생성된 티켓의 만남 ID가 없습니다." },
                memory = checkNotNull(postLog.memory) { "생성된 티켓의 추억 문구가 없습니다." },
                ticketCreated = postLog.isTicketCreated,
            )
    }
}
