package org.com.belog.prelog.repository

import org.com.belog.prelog.domain.PlanLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlanLikeRepository :
    JpaRepository<PlanLike, Long>,
    PlanLikeRepositoryCustom {
    fun countByPlanId(planId: Long): Long

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
