package org.com.belog.meeting.repository

import jakarta.persistence.LockModeType
import org.com.belog.meeting.domain.MeetingScheduleResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MeetingScheduleResponseRepository : JpaRepository<MeetingScheduleResponse, Long> {
    fun findByMeetingIdAndParticipantId(
        meetingId: Long,
        participantId: Long,
    ): MeetingScheduleResponse?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT response
        FROM MeetingScheduleResponse response
        WHERE response.meeting.id = :meetingId
          AND response.participant.id = :participantId
        """,
    )
    fun findByMeetingIdAndParticipantIdForUpdate(
        @Param("meetingId") meetingId: Long,
        @Param("participantId") participantId: Long,
    ): MeetingScheduleResponse?
}
