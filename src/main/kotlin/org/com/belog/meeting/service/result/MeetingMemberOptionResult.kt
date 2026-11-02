package org.com.belog.meeting.service.result

data class MeetingMemberOptionResult(
    val groupMemberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isMe: Boolean,
    val isParticipant: Boolean,
    val isRemovable: Boolean,
)
