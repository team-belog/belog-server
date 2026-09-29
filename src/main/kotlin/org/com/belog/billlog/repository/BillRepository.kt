package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.Bill
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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
