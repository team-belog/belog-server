package org.com.belog.group.service.result

data class GroupDetailResult(
    val groupId: Long,
    val name: String,
    val coverImageUrl: String?,
    val inviteCode: String,
    val memberCount: Int,
    val canEditCoverImage: Boolean,
    val canDeleteGroup: Boolean,
    val schedulingMeetings: List<GroupMeetingResult>,
    val activeMeetings: List<GroupMeetingResult>,
)

data class GroupMeetingResult(
    val meetingId: Long,
    val name: String,
    val isMeetingOwner: Boolean,
    val participants: List<GroupMeetingParticipantResult>,
    val participantCount: Int,
)

data class GroupMeetingParticipantResult(
    val nickname: String,
    val isMeetingOwner: Boolean,
)
