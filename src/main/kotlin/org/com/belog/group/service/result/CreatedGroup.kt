package org.com.belog.group.service.result

data class CreatedGroup(
    val groupId: Long,
    val name: String,
    val coverImageObjectKey: String?,
    val currentMemberCount: Int,
    val inviteCode: String,
    val inviteLink: String,
)
