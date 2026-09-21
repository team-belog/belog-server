package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingScheduleResponse
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingScheduleResponseRepository : JpaRepository<MeetingScheduleResponse, Long> {
    fun findByMeetingIdAndParticipantId(
        meetingId: Long,
        participantId: Long,
    ): MeetingScheduleResponse?

    fun existsByMeetingIdAndParticipantId(
        meetingId: Long,
        participantId: Long,
    ): Boolean
}
