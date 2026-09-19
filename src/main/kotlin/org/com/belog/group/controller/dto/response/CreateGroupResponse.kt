package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.service.result.CreatedGroup

data class CreateGroupResponse(
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "그룹명", example = "주말 러닝 모임")
    val name: String,
    @field:Schema(description = "현재 그룹 인원", example = "1")
    val currentMemberCount: Int,
    @field:Schema(description = "6자리 그룹 초대 코드", example = "AB12CD")
    val inviteCode: String,
    @field:Schema(description = "공유 가능한 그룹 초대 링크", example = "https://belog.co.kr/invitations/AB12CD")
    val inviteLink: String,
) {
    companion object {
        fun from(createdGroup: CreatedGroup): CreateGroupResponse =
            CreateGroupResponse(
                groupId = createdGroup.groupId,
                name = createdGroup.name,
                currentMemberCount = createdGroup.currentMemberCount,
                inviteCode = createdGroup.inviteCode,
                inviteLink = createdGroup.inviteLink,
            )
    }
}
