package org.com.belog.group.service.result

import java.time.LocalDate

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
    val participantNicknames: List<String>,
    val participantCount: Int,
)

data class ActiveMeetingResult(
    val meetingId: Long,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
