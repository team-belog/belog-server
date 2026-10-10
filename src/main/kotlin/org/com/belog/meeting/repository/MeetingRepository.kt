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
        WHERE meeting.id = :meetingId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findActiveById(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    fun existsByIdAndDeletedAtIsNull(id: Long): Boolean

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.group
        WHERE meeting.id = :meetingId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findByIdWithGroup(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.group
        WHERE meeting.id = :meetingId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findByIdWithGroupForUpdate(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.group.id = :groupId
          AND meeting.status = :status
          AND meeting.deletedAt IS NULL
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
          AND meeting.deletedAt IS NULL
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
          AND meeting.deletedAt IS NULL
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
        SELECT meeting.id
        FROM Meeting meeting
        WHERE meeting.startDate = :startDate
          AND meeting.status = :status
          AND (:cursor IS NULL OR meeting.id > :cursor)
          AND meeting.deletedAt IS NULL
        ORDER BY meeting.id ASC
        """,
    )
    fun findIdsByStartDate(
        @Param("startDate") startDate: LocalDate,
        @Param("cursor") cursor: Long?,
        @Param("status") status: MeetingStatus,
        pageable: Pageable,
    ): List<Long>

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.group
        JOIN FETCH meeting.createdBy
        JOIN FETCH meeting.owner
        WHERE meeting.id = :meetingId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findByIdWithGroupOwnerAndCreator(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        JOIN FETCH meeting.owner owner
        JOIN FETCH owner.user
        WHERE meeting.id = :meetingId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findByIdForUpdate(
        @Param("meetingId") meetingId: Long,
    ): Meeting?

    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.owner.user.id = :userId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findAllActiveByOwnerUserId(
        @Param("userId") userId: Long,
    ): List<Meeting>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT meeting
        FROM Meeting meeting
        WHERE meeting.group.id = :groupId
          AND meeting.deletedAt IS NULL
        ORDER BY meeting.id ASC
        """,
    )
    fun findAllByGroupIdForUpdate(
        @Param("groupId") groupId: Long,
    ): List<Meeting>
}
