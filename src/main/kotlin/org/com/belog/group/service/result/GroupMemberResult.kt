package org.com.belog.group.service.result

import org.com.belog.group.domain.GroupRole

data class GroupMemberResult(
    val groupMemberId: Long,
    val nickname: String,
    val name: String,
    val profileImageUrl: String?,
    val role: GroupRole,
)
