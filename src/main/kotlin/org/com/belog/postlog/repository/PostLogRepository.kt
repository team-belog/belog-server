package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PostLogRepository : JpaRepository<PostLog, Long> {
    fun findByMeetingIdAndCreatedById(
        meetingId: Long,
        groupMemberId: Long,
    ): PostLog?

    @Query(
        """
        SELECT postLog
        FROM PostLog postLog
        JOIN FETCH postLog.meeting meeting
        WHERE postLog.id = :postLogId
          AND postLog.createdBy.user.id = :userId
          AND postLog.ticketCreatedAt IS NOT NULL
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findTicketByIdAndUserId(
        @Param("postLogId") postLogId: Long,
        @Param("userId") userId: Long,
    ): PostLog?

    @Query(
        """
        SELECT postLog
        FROM PostLog postLog
        JOIN FETCH postLog.meeting meeting
        WHERE postLog.createdBy.user.id = :userId
          AND postLog.ticketCreatedAt IS NOT NULL
          AND meeting.endDate BETWEEN :startDate AND :endDate
          AND meeting.deletedAt IS NULL
        ORDER BY meeting.endDate ASC, postLog.id ASC
        """,
    )
    fun findTicketCalendar(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<PostLog>
}
