package org.com.belog.meeting.service.result

import org.com.belog.meeting.domain.MeetingLogStatus
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import java.time.LocalDate

data class MeetingDetailResult(
    val meetingId: Long,
    val groupId: Long,
    val groupName: String,
    val meetingName: String,
    val scheduleType: MeetingScheduleType,
    val meetingStatus: MeetingStatus,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val location: String?,
    val canEditMeeting: Boolean,
    val preLogStatus: MeetingLogStatus,
    val billLogStatus: MeetingLogStatus,
    val postLogStatus: MeetingLogStatus,
)
