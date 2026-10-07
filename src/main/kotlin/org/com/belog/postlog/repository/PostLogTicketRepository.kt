package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface PostLogTicketRepository : JpaRepository<PostLogTicket, Long> {
    fun findBySourceMeetingIdAndCreatorGroupMemberId(
        sourceMeetingId: Long,
        creatorGroupMemberId: Long,
    ): PostLogTicket?

    fun existsBySourceMeetingIdAndCreatorGroupMemberId(
        sourceMeetingId: Long,
        creatorGroupMemberId: Long,
    ): Boolean

    fun findByIdAndOwnerId(
        id: Long,
        ownerId: Long,
    ): PostLogTicket?

    @Query(
        """
        SELECT ticket
        FROM PostLogTicket ticket
        WHERE ticket.owner.id = :userId
          AND ticket.meetingEndDate BETWEEN :startDate AND :endDate
        ORDER BY ticket.meetingEndDate ASC, ticket.id ASC
        """,
    )
    fun findCalendar(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<PostLogTicket>
}
