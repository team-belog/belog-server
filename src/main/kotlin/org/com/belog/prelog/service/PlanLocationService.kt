package org.com.belog.prelog.service

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.infrastructure.PlanLocationResolver
import org.com.belog.prelog.service.result.PlanLocationResolution
import org.springframework.stereotype.Service

@Service
class PlanLocationService(
    private val planService: PlanService,
    private val planMapUrlDetector: PlanMapUrlDetector,
    private val planLocationResolvers: List<PlanLocationResolver>,
) {
    fun createLocationPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        planService.validatePlanCreation(meetingId = meetingId, userId = creatorUserId)
        val normalizedUrl = url.trim()
        val resolution =
            planMapUrlDetector
                .detect(normalizedUrl)
                ?.let { provider -> resolveLocation(normalizedUrl, provider) }
                ?: PlanLocationResolution.NotApplicable

        return planService.createLocationPlan(
            meetingId = meetingId,
            creatorUserId = creatorUserId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
        )
    }

    private fun resolveLocation(
        url: String,
        provider: MapProvider,
    ): PlanLocationResolution {
        val resolver = planLocationResolvers.firstOrNull { candidate -> candidate.provider == provider }
        val resolution = resolver?.resolve(url)

        return when (resolution) {
            null, PlanLocationResolution.NotApplicable -> PlanLocationResolution.ProviderFailed(provider)
            else -> resolution
        }
    }
}
