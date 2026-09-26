package org.com.belog.prelog.repository

import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlanRepository : JpaRepository<Plan, Long> {
    fun existsByMeetingId(meetingId: Long): Boolean

    fun findByIdAndMeetingId(
        planId: Long,
        meetingId: Long,
    ): Plan?

    @Query(
        """
        SELECT plan
        FROM Plan plan
        JOIN FETCH plan.createdBy
        WHERE plan.meeting.id = :meetingId
          AND (:category IS NULL OR plan.category = :category)
          AND (:cursor IS NULL OR plan.id < :cursor)
        ORDER BY plan.id DESC
        """,
    )
    fun findPageWithCreator(
        @Param("meetingId") meetingId: Long,
        @Param("category") category: PlanCategory?,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Plan>
}
