package org.com.belog.home.repository

import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface HomeCalendarRepository : Repository<Meeting, Long> {
    @Query(
        """
        SELECT meeting
        FROM MeetingParticipant participant
        JOIN participant.meeting meeting
        WHERE participant.groupMember.user.id = :userId
          AND meeting.status = :status
          AND meeting.startDate <= :monthEnd
          AND meeting.endDate >= :monthStart
          AND meeting.deletedAt IS NULL
        ORDER BY meeting.startDate ASC, meeting.id ASC
        """,
    )
    fun findMeetingsOverlappingMonth(
        @Param("userId") userId: Long,
        @Param("status") status: MeetingStatus,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<Meeting>
}
