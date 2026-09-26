package org.com.belog.prelog.repository

import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

open class PlanLikeRepositoryCustomImpl(
    private val entityManager: EntityManager,
) : PlanLikeRepositoryCustom {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun saveIfAbsent(
        planId: Long,
        groupMemberId: Long,
    ) {
        entityManager
            .createNativeQuery(SAVE_IF_ABSENT_SQL)
            .setParameter("planId", planId)
            .setParameter("groupMemberId", groupMemberId)
            .executeUpdate()
    }

    companion object {
        private const val SAVE_IF_ABSENT_SQL =
            """
            INSERT INTO pre_log_plan_likes (
                plan_id,
                group_member_id,
                created_at,
                updated_at
            ) VALUES (
                :planId,
                :groupMemberId,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE id = id
            """
    }
}
