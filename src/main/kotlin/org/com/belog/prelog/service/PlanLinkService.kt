package org.com.belog.prelog.service

import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.springframework.stereotype.Service

@Service
class PlanLinkService(
    private val planService: PlanService,
    private val planLocationResolver: PlanLocationResolver,
) {
    fun createLinkPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        planService.validatePlanCreation(meetingId = meetingId, userId = creatorUserId)
        val resolution = planLocationResolver.resolve(url)

        return planService.createLinkPlan(
            meetingId = meetingId,
            creatorUserId = creatorUserId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
        )
    }

    fun updateLinkPlan(
        planId: Long,
        userId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        val currentUrl = planService.getLinkUrlForUpdate(planId = planId, userId = userId)
        val normalizedUrl = url.trim()
        val resolution =
            if (currentUrl == normalizedUrl) {
                null
            } else {
                planLocationResolver.resolve(normalizedUrl)
            }

        return planService.updateLinkPlan(
            planId = planId,
            userId = userId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
        )
    }
}
