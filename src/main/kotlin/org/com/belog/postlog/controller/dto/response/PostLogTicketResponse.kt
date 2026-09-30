package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogTicketMemberResult
import org.com.belog.postlog.service.result.PostLogTicketResult
import java.time.LocalDate

@Schema(description = "Post-log 티켓 생성 및 조회 결과")
data class PostLogTicketResponse(
    @field:Schema(description = "개인 Post-log 티켓 ID", example = "11")
    val postLogId: Long,
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val meetingName: String,
    @field:Schema(description = "티켓에 들어가는 추억 문구", example = "함께한 광주 여행을 오래 기억하자")
    val memory: String,
    @field:Schema(description = "대표 사진 URL. 등록된 사진이 없으면 null", nullable = true)
    val coverPhotoUrl: String?,
    @field:Schema(description = "만남 시작일", example = "2026-08-17", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "만남 종료일", example = "2026-08-18", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "만남 장소", example = "대한민국 광주", nullable = true)
    val location: String?,
    @field:Schema(description = "만남 참여 멤버")
    val members: List<PostLogTicketMemberResponse>,
) {
    companion object {
        fun from(result: PostLogTicketResult): PostLogTicketResponse =
            PostLogTicketResponse(
                postLogId = result.postLogId,
                meetingId = result.meetingId,
                meetingName = result.meetingName,
                memory = result.memory,
                coverPhotoUrl = result.coverPhotoUrl,
                startDate = result.startDate,
                endDate = result.endDate,
                location = result.location,
                members = result.members.map(PostLogTicketMemberResponse::from),
            )
    }
}

@Schema(description = "Post-log 티켓 참여 멤버")
data class PostLogTicketMemberResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "닉네임", example = "thisgarten")
    val nickname: String,
) {
    companion object {
        fun from(result: PostLogTicketMemberResult): PostLogTicketMemberResponse =
            PostLogTicketMemberResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
            )
    }
}
