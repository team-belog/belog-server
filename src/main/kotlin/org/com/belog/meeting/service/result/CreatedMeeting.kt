package org.com.belog.meeting.service.result

import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import java.time.Instant
import java.time.LocalDate

data class CreatedMeeting(
    val meetingId: Long,
    val groupId: Long,
    val name: String,
    val location: String?,
    val scheduleType: MeetingScheduleType,
    val status: MeetingStatus,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val confirmedAt: Instant?,
    val participantCount: Int,
)
