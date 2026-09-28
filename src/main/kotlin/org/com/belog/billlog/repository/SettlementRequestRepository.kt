package org.com.belog.billlog.repository

import jakarta.persistence.LockModeType
import org.com.belog.billlog.domain.SettlementRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SettlementRequestRepository : JpaRepository<SettlementRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT settlementRequest
        FROM SettlementRequest settlementRequest
        JOIN FETCH settlementRequest.bill bill
        JOIN FETCH bill.payer payer
        JOIN FETCH payer.groupMember groupMember
        JOIN FETCH groupMember.user
        WHERE settlementRequest.id = :settlementRequestId
          AND bill.meeting.id = :meetingId
        """,
    )
    fun findByIdAndMeetingIdForUpdate(
        @Param("settlementRequestId") settlementRequestId: Long,
        @Param("meetingId") meetingId: Long,
    ): SettlementRequest?
}
