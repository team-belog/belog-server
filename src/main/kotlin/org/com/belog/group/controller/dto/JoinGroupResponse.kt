package org.com.belog.group.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.service.result.JoinedGroup

data class JoinGroupResponse(
    @field:Schema(description = "참여한 그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "참여한 그룹명", example = "주말 러닝 모임")
    val name: String,
    @field:Schema(description = "참여 후 현재 그룹 인원", example = "8")
    val currentMemberCount: Int,
) {
    companion object {
        fun from(joinedGroup: JoinedGroup): JoinGroupResponse =
            JoinGroupResponse(
                groupId = joinedGroup.groupId,
                name = joinedGroup.name,
                currentMemberCount = joinedGroup.currentMemberCount,
            )
    }
}
