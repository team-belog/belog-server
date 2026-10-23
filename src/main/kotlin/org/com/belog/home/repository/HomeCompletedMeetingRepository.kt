package org.com.belog.home.repository

import org.com.belog.postlog.domain.PostLogTicket
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface HomeCompletedMeetingRepository : Repository<PostLogTicket, Long> {
    @Query(
        """
        SELECT ticket
        FROM PostLogTicket ticket
        WHERE ticket.owner.id = :userId
          AND ticket.meetingEndDate IS NOT NULL
        ORDER BY ticket.meetingEndDate DESC, ticket.id DESC
        """,
    )
    fun findCompletedPage(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): List<PostLogTicket>

    @Query(
        """
        SELECT ticket
        FROM PostLogTicket ticket
        WHERE ticket.owner.id = :userId
          AND ticket.meetingEndDate IS NOT NULL
          AND (
              ticket.meetingEndDate < :cursorEndDate
              OR (ticket.meetingEndDate = :cursorEndDate AND ticket.id < :cursorTicketId)
          )
        ORDER BY ticket.meetingEndDate DESC, ticket.id DESC
        """,
    )
    fun findCompletedPageAfter(
        @Param("userId") userId: Long,
        @Param("cursorEndDate") cursorEndDate: LocalDate,
        @Param("cursorTicketId") cursorTicketId: Long,
        pageable: Pageable,
    ): List<PostLogTicket>
}
