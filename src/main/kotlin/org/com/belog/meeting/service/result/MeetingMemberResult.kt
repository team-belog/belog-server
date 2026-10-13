package org.com.belog.meeting.service.result

data class MeetingMemberResult(
    val groupMemberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isMe: Boolean,
)
