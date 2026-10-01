package org.com.belog.home.repository

import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface HomeActiveMeetingRepository : Repository<Meeting, Long> {
    @Query(
        """
        SELECT meeting
        FROM MeetingParticipant participant
        JOIN participant.meeting meeting
        JOIN FETCH meeting.group
        WHERE participant.groupMember.user.id = :userId
          AND (
              meeting.status = :schedulingStatus
              OR (meeting.status = :confirmedStatus AND meeting.endDate >= :currentDate)
          )
          AND meeting.deletedAt IS NULL
          AND (
              :query = ''
              OR LOWER(meeting.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(meeting.group.name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY
            CASE WHEN meeting.status = :schedulingStatus THEN 0 ELSE 1 END ASC,
            meeting.startDate ASC,
            meeting.id ASC
        """,
    )
    fun findActivePage(
        @Param("userId") userId: Long,
        @Param("schedulingStatus") schedulingStatus: MeetingStatus,
        @Param("confirmedStatus") confirmedStatus: MeetingStatus,
        @Param("currentDate") currentDate: LocalDate,
        @Param("query") query: String,
        pageable: Pageable,
    ): List<Meeting>

    @Query(
        """
        SELECT meeting
        FROM MeetingParticipant participant
        JOIN participant.meeting meeting
        JOIN FETCH meeting.group
        WHERE participant.groupMember.user.id = :userId
          AND (
              (meeting.status = :schedulingStatus AND meeting.id > :cursorMeetingId)
              OR (meeting.status = :confirmedStatus AND meeting.endDate >= :currentDate)
          )
          AND meeting.deletedAt IS NULL
          AND (
              :query = ''
              OR LOWER(meeting.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(meeting.group.name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY
            CASE WHEN meeting.status = :schedulingStatus THEN 0 ELSE 1 END ASC,
            meeting.startDate ASC,
            meeting.id ASC
        """,
    )
    fun findActivePageAfterScheduling(
        @Param("userId") userId: Long,
        @Param("schedulingStatus") schedulingStatus: MeetingStatus,
        @Param("confirmedStatus") confirmedStatus: MeetingStatus,
        @Param("currentDate") currentDate: LocalDate,
        @Param("query") query: String,
        @Param("cursorMeetingId") cursorMeetingId: Long,
        pageable: Pageable,
    ): List<Meeting>

    @Query(
        """
        SELECT meeting
        FROM MeetingParticipant participant
        JOIN participant.meeting meeting
        JOIN FETCH meeting.group
        WHERE participant.groupMember.user.id = :userId
          AND meeting.status = :confirmedStatus
          AND meeting.endDate >= :currentDate
          AND meeting.deletedAt IS NULL
          AND (
              :query = ''
              OR LOWER(meeting.name) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(meeting.group.name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
          AND (
              meeting.startDate > :cursorStartDate
              OR (meeting.startDate = :cursorStartDate AND meeting.id > :cursorMeetingId)
          )
        ORDER BY meeting.startDate ASC, meeting.id ASC
        """,
    )
    fun findActivePageAfterConfirmed(
        @Param("userId") userId: Long,
        @Param("confirmedStatus") confirmedStatus: MeetingStatus,
        @Param("currentDate") currentDate: LocalDate,
        @Param("query") query: String,
        @Param("cursorStartDate") cursorStartDate: LocalDate,
        @Param("cursorMeetingId") cursorMeetingId: Long,
        pageable: Pageable,
    ): List<Meeting>
}
