package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.service.result.GroupMemberResult

data class GroupMembersResponse(
    @field:Schema(description = "요청자 우선, 나머지는 OWNER 우선 및 그룹 멤버 ID 오름차순으로 정렬된 그룹 멤버 목록")
    val items: List<GroupMemberResponse>,
) {
    companion object {
        fun from(results: List<GroupMemberResult>): GroupMembersResponse =
            GroupMembersResponse(
                items = results.map(GroupMemberResponse::from),
            )
    }
}
