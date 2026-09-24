package org.com.belog.meeting.repository

import jakarta.persistence.LockModeType
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MeetingRepository : JpaRepository<Meeting, Long> {
    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.group.id = :groupId
          AND meeting.status = :status
        ORDER BY meeting.id DESC
        """,
    )
    fun findSchedulingMeetings(
        @Param("groupId") groupId: Long,
        @Param("status") status: MeetingStatus,
    ): List<Meeting>

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.group.id = :groupId
          AND meeting.status = :status
          AND meeting.endDate >= :currentDate
        ORDER BY meeting.startDate ASC, meeting.id ASC
        """,
    )
    fun findActiveMeetings(
        @Param("groupId") groupId: Long,
        @Param("currentDate") currentDate: LocalDate,
        @Param("status") status: MeetingStatus,
    ): List<Meeting>

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.group.id = :groupId
          AND meeting.status = :status
          AND meeting.endDate < :currentDate
          AND (:cursor IS NULL OR meeting.id < :cursor)
        ORDER BY meeting.id DESC
        """,
    )
    fun findPastMeetingPage(
        @Param("groupId") groupId: Long,
        @Param("currentDate") currentDate: LocalDate,
        @Param("cursor") cursor: Long?,
        @Param("status") status: MeetingStatus,
        pageable: Pageable,
    ): List<Meeting>

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.group
        JOIN FETCH meeting.createdBy
        WHERE meeting.id = :meetingId
        """,
    )
    fun findByIdWithGroupAndCreator(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.createdBy creator
        JOIN FETCH creator.user
        WHERE meeting.id = :meetingId
        """,
    )
    fun findByIdForUpdate(
        @Param("meetingId") meetingId: Long,
    ): Meeting?
}
