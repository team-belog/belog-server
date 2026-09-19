package org.com.belog.group.service.result

data class JoinedGroup(
    val groupId: Long,
    val name: String,
    val currentMemberCount: Int,
)
