package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.Bill
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDate

interface BillRepository : JpaRepository<Bill, Long> {
    fun existsByMeetingId(meetingId: Long): Boolean

    @Query(
        """
        SELECT COALESCE(SUM(bill.totalAmount), 0)
        FROM Bill bill
        WHERE bill.meeting.id = :meetingId
        """,
    )
    fun sumTotalAmountByMeetingId(
        @Param("meetingId") meetingId: Long,
    ): Long

    @Query(
        """
        SELECT DISTINCT CAST(bill.createdAt AS LocalDate)
        FROM Bill bill
        WHERE bill.meeting.id = :meetingId
          AND (
              :cursorDate IS NULL
              OR CAST(bill.createdAt AS LocalDate) < :cursorDate
          )
        ORDER BY CAST(bill.createdAt AS LocalDate) DESC
        """,
    )
    fun findPaymentDatePage(
        @Param("meetingId") meetingId: Long,
        @Param("cursorDate") cursorDate: LocalDate?,
        pageable: Pageable,
    ): List<LocalDate>

    @Query(
        """
        SELECT bill
        FROM Bill bill
        JOIN FETCH bill.payer payer
        JOIN FETCH payer.groupMember payerMember
        JOIN FETCH payerMember.user
        WHERE bill.meeting.id = :meetingId
          AND bill.createdAt >= :fromInclusive
          AND bill.createdAt < :toExclusive
        ORDER BY bill.createdAt DESC, bill.id DESC
        """,
    )
    fun findAllWithPayerByMeetingIdAndCreatedAtRange(
        @Param("meetingId") meetingId: Long,
        @Param("fromInclusive") fromInclusive: Instant,
        @Param("toExclusive") toExclusive: Instant,
    ): List<Bill>

    @Query(
        """
        SELECT bill
        FROM Bill bill
        JOIN FETCH bill.meeting meeting
        JOIN FETCH meeting.group
        JOIN FETCH bill.payer payer
        JOIN FETCH payer.groupMember payerMember
        JOIN FETCH payerMember.user
        WHERE bill.id = :billId
          AND meeting.id = :meetingId
        """,
    )
    fun findByIdAndMeetingIdWithPayer(
        @Param("billId") billId: Long,
        @Param("meetingId") meetingId: Long,
    ): Bill?
}
