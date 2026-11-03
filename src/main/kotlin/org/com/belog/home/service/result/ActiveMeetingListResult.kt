package org.com.belog.home.service.result

import org.com.belog.home.domain.HomeMeetingProgressStatus
import org.com.belog.home.service.query.ActiveMeetingCursor
import java.time.LocalDate

data class ActiveMeetingListResult(
    val items: List<ActiveMeetingResult>,
    val nextCursor: ActiveMeetingCursor?,
    val hasNext: Boolean,
)

data class ActiveMeetingResult(
    val meetingId: Long,
    val name: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val groupName: String,
    val progressStatus: HomeMeetingProgressStatus,
    val participantCount: Int,
    val previewParticipants: List<ActiveMeetingParticipantResult>,
)

data class ActiveMeetingParticipantResult(
    val groupMemberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
