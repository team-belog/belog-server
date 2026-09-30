package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogParticipantResult
import org.com.belog.postlog.service.result.PostLogSummaryResult
import java.time.LocalDate

@Schema(description = "Post-log 만남 정리 조회 결과")
data class PostLogSummaryResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val meetingName: String,
    @field:Schema(description = "시작일", example = "2026-08-17", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "종료일", example = "2026-08-18", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "장소", example = "대한민국 광주", nullable = true)
    val location: String?,
    @field:Schema(description = "티켓에 들어갈 추억 문구", example = "함께한 광주 여행을 오래 기억하자", nullable = true)
    val memory: String?,
    @field:Schema(description = "정산 요약")
    val settlement: PostLogSettlementSummaryResponse,
    @field:Schema(description = "참여 멤버 수", example = "3")
    val participantCount: Int,
    @field:Schema(description = "참여 멤버 목록")
    val participants: List<PostLogParticipantResponse>,
    @field:Schema(description = "티켓 생성 여부", example = "false")
    val ticketCreated: Boolean,
) {
    companion object {
        fun from(result: PostLogSummaryResult): PostLogSummaryResponse =
            PostLogSummaryResponse(
                meetingId = result.meetingId,
                meetingName = result.meetingName,
                startDate = result.startDate,
                endDate = result.endDate,
                location = result.location,
                memory = result.memory,
                settlement =
                    PostLogSettlementSummaryResponse(
                        completedParticipantCount = result.completedParticipantCount,
                        totalAmount = result.totalAmount,
                    ),
                participantCount = result.participants.size,
                participants = result.participants.map(PostLogParticipantResponse::from),
                ticketCreated = result.ticketCreated,
            )
    }
}

@Schema(description = "Post-log 정산 요약")
data class PostLogSettlementSummaryResponse(
    @field:Schema(description = "모든 정산 요청을 완료한 참여자 수", example = "1")
    val completedParticipantCount: Long,
    @field:Schema(description = "전체 결제 금액", example = "11000")
    val totalAmount: Long,
)

@Schema(description = "Post-log 참여 멤버")
data class PostLogParticipantResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "닉네임", example = "이정원")
    val nickname: String,
    @field:Schema(description = "만남 생성자 여부", example = "true")
    val meetingCreator: Boolean,
) {
    companion object {
        fun from(result: PostLogParticipantResult): PostLogParticipantResponse =
            PostLogParticipantResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                meetingCreator = result.meetingCreator,
            )
    }
}
