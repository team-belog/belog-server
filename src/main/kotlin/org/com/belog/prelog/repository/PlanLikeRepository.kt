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
    @Query(
        """
        SELECT new org.com.belog.prelog.repository.PlanLikeCount(
            planLike.plan.id,
            COUNT(planLike.id)
        )
        FROM PlanLike planLike
        WHERE planLike.plan.id IN :planIds
        GROUP BY planLike.plan.id
        """,
    )
    fun countByPlanIds(
        @Param("planIds") planIds: Collection<Long>,
    ): List<PlanLikeCount>

    @Query(
        """
        SELECT planLike.plan.id
        FROM PlanLike planLike
        WHERE planLike.plan.id IN :planIds
          AND planLike.groupMember.id = :groupMemberId
        """,
    )
    fun findLikedPlanIds(
        @Param("planIds") planIds: Collection<Long>,
        @Param("groupMemberId") groupMemberId: Long,
    ): List<Long>

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

    @Modifying
    @Query(
        """
        DELETE FROM PlanLike planLike
        WHERE planLike.plan.id = :planId
        """,
    )
    fun deleteAllByPlanId(
        @Param("planId") planId: Long,
    ): Int
}
