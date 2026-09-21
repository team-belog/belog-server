package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingAvailableDate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MeetingAvailableDateRepository : JpaRepository<MeetingAvailableDate, Long> {
    @Query(
        """
        SELECT availableDate
        FROM MeetingAvailableDate availableDate
        JOIN FETCH availableDate.response response
        JOIN FETCH response.participant participant
        JOIN FETCH availableDate.candidateDateRange candidate
        WHERE response.meeting.id = :meetingId
        ORDER BY participant.id ASC, candidate.createdAt ASC, candidate.id ASC
        """,
    )
    fun findAllWithResponseParticipantAndCandidateByMeetingId(
        @Param("meetingId") meetingId: Long,
    ): List<MeetingAvailableDate>

    @Query(
        """
        SELECT availableDate
        FROM MeetingAvailableDate availableDate
        JOIN FETCH availableDate.candidateDateRange candidate
        WHERE availableDate.response.id = :responseId
        ORDER BY candidate.startDate ASC, candidate.endDate ASC, candidate.id ASC
        """,
    )
    fun findAllWithCandidateByResponseId(
        @Param("responseId") responseId: Long,
    ): List<MeetingAvailableDate>
}
