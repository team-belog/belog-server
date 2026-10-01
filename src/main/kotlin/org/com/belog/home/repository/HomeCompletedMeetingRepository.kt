package org.com.belog.home.repository

import org.com.belog.postlog.domain.PostLog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface HomeCompletedMeetingRepository : Repository<PostLog, Long> {
    @Query(
        """
        SELECT postLog
        FROM PostLog postLog
        JOIN FETCH postLog.meeting meeting
        WHERE postLog.createdBy.user.id = :userId
          AND postLog.ticketCreatedAt IS NOT NULL
          AND meeting.endDate < :currentDate
          AND meeting.deletedAt IS NULL
        ORDER BY meeting.endDate DESC, postLog.id DESC
        """,
    )
    fun findCompletedPage(
        @Param("userId") userId: Long,
        @Param("currentDate") currentDate: LocalDate,
        pageable: Pageable,
    ): List<PostLog>

    @Query(
        """
        SELECT postLog
        FROM PostLog postLog
        JOIN FETCH postLog.meeting meeting
        WHERE postLog.createdBy.user.id = :userId
          AND postLog.ticketCreatedAt IS NOT NULL
          AND meeting.endDate < :currentDate
          AND meeting.deletedAt IS NULL
          AND (
              meeting.endDate < :cursorEndDate
              OR (meeting.endDate = :cursorEndDate AND postLog.id < :cursorPostLogId)
          )
        ORDER BY meeting.endDate DESC, postLog.id DESC
        """,
    )
    fun findCompletedPageAfter(
        @Param("userId") userId: Long,
        @Param("currentDate") currentDate: LocalDate,
        @Param("cursorEndDate") cursorEndDate: LocalDate,
        @Param("cursorPostLogId") cursorPostLogId: Long,
        pageable: Pageable,
    ): List<PostLog>
}
