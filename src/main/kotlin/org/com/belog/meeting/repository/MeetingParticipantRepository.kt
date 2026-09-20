package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingParticipantRepository : JpaRepository<MeetingParticipant, Long> {
    fun countByMeetingId(meetingId: Long): Long

    fun existsByMeetingIdAndGroupMemberId(
        meetingId: Long,
        groupMemberId: Long,
    ): Boolean
}
