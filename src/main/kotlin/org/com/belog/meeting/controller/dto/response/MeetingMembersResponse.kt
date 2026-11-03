package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.service.result.MeetingMemberResult

data class MeetingMembersResponse(
    @field:Schema(description = "요청자 우선, 나머지는 그룹 멤버 ID 오름차순으로 정렬된 만남 참여 멤버 목록")
    val items: List<MeetingMemberResponse>,
) {
    companion object {
        fun from(results: List<MeetingMemberResult>): MeetingMembersResponse =
            MeetingMembersResponse(items = results.map(MeetingMemberResponse::from))
    }
}

data class MeetingMemberResponse(
    @field:Schema(description = "그룹 멤버 ID. 결제 등록의 payerMemberId 및 participantMemberId에 사용하는 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "멤버 닉네임", example = "빌로그")
    val nickname: String,
    @field:Schema(description = "프로필 이미지 조회 URL. 프로필 이미지가 없으면 null", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "요청자 본인 여부", example = "true")
    val isMe: Boolean,
) {
    companion object {
        fun from(result: MeetingMemberResult): MeetingMemberResponse =
            MeetingMemberResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                isMe = result.isMe,
            )
    }
}
