package org.com.belog.prelog.repository

import jakarta.persistence.LockModeType
import org.com.belog.prelog.domain.PlanLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlanLikeRepository :
    JpaRepository<PlanLike, Long>,
    PlanLikeRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
        """
        SELECT planLike
        FROM PlanLike planLike
        WHERE planLike.plan.id = :planId
        """,
    )
    fun findAllByPlanIdForShare(
        @Param("planId") planId: Long,
    ): List<PlanLike>

    @Modifying
    @Query(
        """
        DELETE FROM PlanLike planLike
        WHERE planLike.plan.id = :planId
          AND planLike.groupMember.id = :groupMemberId
        """,
    )
    fun deleteByPlanIdAndGroupMemberId(
        @Param("planId") planId: Long,
        @Param("groupMemberId") groupMemberId: Long,
    ): Int
}
