package org.com.belog.group.service.result

data class GroupDetailResult(
    val groupId: Long,
    val name: String,
    val coverImageUrl: String?,
    val inviteCode: String,
    val memberCount: Int,
    val canEditCoverImage: Boolean,
    val canDeleteGroup: Boolean,
    val schedulingMeetings: List<SchedulingMeetingResult>,
    val activeMeetings: List<ActiveMeetingResult>,
)

data class SchedulingMeetingResult(
    val meetingId: Long,
    val name: String,
    val isMeetingOwner: Boolean,
    val hasResponded: Boolean,
    val participants: List<GroupMeetingParticipantResult>,
    val participantCount: Int,
)

data class ActiveMeetingResult(
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
