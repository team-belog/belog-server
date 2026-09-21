package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MeetingCandidateDateRangeRepository : JpaRepository<MeetingCandidateDateRange, Long> {
    @Query(
        """
        SELECT candidate
        FROM MeetingCandidateDateRange candidate
        WHERE candidate.meeting.id = :meetingId
        ORDER BY candidate.startDate ASC, candidate.endDate ASC, candidate.id ASC
        """,
    )
    fun findAllByMeetingIdOrderByDate(
        @Param("meetingId") meetingId: Long,
    ): List<MeetingCandidateDateRange>

    fun findAllByMeetingIdAndIdIn(
        meetingId: Long,
        candidateDateRangeIds: Collection<Long>,
    ): List<MeetingCandidateDateRange>
}
