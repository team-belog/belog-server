package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.service.result.GroupMemberResult

data class GroupMemberResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "멤버 닉네임", example = "빌로그")
    val nickname: String,
    @field:Schema(
        description = "프로필 이미지 조회 URL. 프로필 이미지가 없으면 null",
        example = "https://belog-test-storage.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp?...",
        nullable = true,
    )
    val profileImageUrl: String?,
    @field:Schema(description = "그룹 역할", example = "MEMBER")
    val role: GroupRole,
) {
    companion object {
        fun from(result: GroupMemberResult): GroupMemberResponse =
            GroupMemberResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                role = result.role,
            )
    }
}
