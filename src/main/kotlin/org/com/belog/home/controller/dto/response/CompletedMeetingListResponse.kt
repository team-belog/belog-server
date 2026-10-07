package org.com.belog.home.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.home.controller.cursor.CompletedMeetingCursorCodec
import org.com.belog.home.service.result.CompletedMeetingListResult
import org.com.belog.home.service.result.CompletedMeetingParticipantResult
import org.com.belog.home.service.result.CompletedMeetingResult
import java.time.LocalDate

@Schema(description = "종료된 만남 티켓 목록 조회 결과")
data class CompletedMeetingListResponse(
    @field:Schema(description = "종료된 만남 티켓 목록")
    val items: List<CompletedMeetingResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: CompletedMeetingListResult): CompletedMeetingListResponse =
            CompletedMeetingListResponse(
                items = result.items.map(CompletedMeetingResponse::from),
                nextCursor = result.nextCursor?.let(CompletedMeetingCursorCodec::encode),
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "종료된 만남 티켓")
data class CompletedMeetingResponse(
    @field:Schema(description = "개인 Post-log 티켓 ID", example = "31")
    val ticketId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val name: String,
    @field:Schema(description = "추억 문구", example = "친구들과 다녀온 첫 여행")
    val memory: String,
    @field:Schema(description = "대표 사진 URL. 등록된 사진이 없으면 null", nullable = true)
    val coverPhotoUrl: String?,
    @field:Schema(description = "시작일", example = "2026-07-17")
    val startDate: LocalDate,
    @field:Schema(description = "종료일", example = "2026-07-18")
    val endDate: LocalDate,
    @field:Schema(description = "장소", example = "광주", nullable = true)
    val location: String?,
    @field:Schema(description = "전체 참여자 수", example = "3")
    val participantCount: Int,
    @field:Schema(description = "참여자 목록")
    val participants: List<CompletedMeetingParticipantResponse>,
) {
    companion object {
        fun from(result: CompletedMeetingResult): CompletedMeetingResponse =
            CompletedMeetingResponse(
                ticketId = result.ticketId,
                name = result.name,
                memory = result.memory,
                coverPhotoUrl = result.coverPhotoUrl,
                startDate = result.startDate,
                endDate = result.endDate,
                location = result.location,
                participantCount = result.participantCount,
                participants = result.participants.map(CompletedMeetingParticipantResponse::from),
            )
    }
}

@Schema(description = "종료된 만남 참여자")
data class CompletedMeetingParticipantResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "닉네임", example = "이정원")
    val nickname: String,
) {
    companion object {
        fun from(result: CompletedMeetingParticipantResult): CompletedMeetingParticipantResponse =
            CompletedMeetingParticipantResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
            )
    }
}
