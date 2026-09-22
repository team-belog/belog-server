package org.com.belog.prelog.service.result

import org.com.belog.meeting.domain.MeetingStatus
import java.time.LocalDate

data class PreLogMainResult(
    val meetingId: Long,
    val meetingName: String,
    val groupId: Long,
    val groupName: String,
    val meetingStatus: MeetingStatus,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val location: String?,
    val isEnded: Boolean,
    val canEditMeeting: Boolean,
)
