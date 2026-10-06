package org.com.belog.prelog.service

import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.springframework.stereotype.Service

@Service
class PlanLinkService(
    private val planService: PlanService,
    private val planMapUrlDetector: PlanMapUrlDetector,
    private val planLocationResolvers: List<PlanLocationResolver>,
) {
    fun createLinkPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        planService.validatePlanCreation(meetingId = meetingId, userId = creatorUserId)
        val resolution = resolveLocation(url)

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
        val unchangedUrl = currentUrl.takeIf { it == normalizedUrl }
        val resolution =
            if (unchangedUrl != null) {
                null
            } else {
                resolveLocation(normalizedUrl)
            }

        return planService.updateLinkPlan(
            planId = planId,
            userId = userId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
            expectedUrlWhenResolutionSkipped = unchangedUrl,
        )
    }

    private fun resolveLocation(url: String): PlanLocationResolution {
        val provider = planMapUrlDetector.detect(url) ?: return PlanLocationResolution.NotApplicable
        val resolver = planLocationResolvers.firstOrNull { candidate -> candidate.provider == provider }

        return resolver?.resolve(url) ?: PlanLocationResolution.NotApplicable
    }
}
