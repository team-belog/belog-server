package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.service.result.MeetingMemberOptionResult

data class MeetingMemberOptionsResponse(
    @field:Schema(description = "요청자 우선, 나머지는 그룹 멤버 ID 오름차순으로 정렬된 그룹 멤버 목록")
    val items: List<MeetingMemberOptionResponse>,
) {
    companion object {
        fun from(results: List<MeetingMemberOptionResult>): MeetingMemberOptionsResponse =
            MeetingMemberOptionsResponse(items = results.map(MeetingMemberOptionResponse::from))
    }
}

data class MeetingMemberOptionResponse(
    @field:Schema(description = "그룹 멤버 ID. 참여자 수정 요청의 groupMemberIds에 사용하는 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "멤버 닉네임", example = "빌로그")
    val nickname: String,
    @field:Schema(description = "프로필 이미지 조회 URL. 프로필 이미지가 없으면 null", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "요청자 본인 여부", example = "true")
    val isMe: Boolean,
    @field:Schema(description = "현재 만남 참여 여부", example = "true")
    val isParticipant: Boolean,
    @field:Schema(
        description = "만남에서 제외할 수 있는지 여부. 방장, 만남 생성자, Bill-log 기록이 있는 참여자와 미참여자는 false",
        example = "false",
    )
    val isRemovable: Boolean,
) {
    companion object {
        fun from(result: MeetingMemberOptionResult): MeetingMemberOptionResponse =
            MeetingMemberOptionResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                isMe = result.isMe,
                isParticipant = result.isParticipant,
                isRemovable = result.isRemovable,
            )
    }
}
