package org.com.belog.meeting.repository

import jakarta.persistence.LockModeType
import org.com.belog.meeting.domain.Meeting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MeetingRepository : JpaRepository<Meeting, Long> {
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
