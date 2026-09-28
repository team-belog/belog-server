package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.BillShare
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BillShareRepository : JpaRepository<BillShare, Long> {
    @Query(
        """
        SELECT share
        FROM BillShare share
        JOIN FETCH share.participant participant
        JOIN FETCH participant.groupMember groupMember
        JOIN FETCH groupMember.user
        WHERE share.bill.id = :billId
        ORDER BY share.allocationOrder ASC
        """,
    )
    fun findAllWithParticipantAndUserByBillId(
        @Param("billId") billId: Long,
    ): List<BillShare>
}
