package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
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
        val provider = detectMapProvider(normalizedUrl)
        val resolution = resolveLocation(normalizedUrl, provider)

        return planService.createLocationPlan(
            meetingId = meetingId,
            creatorUserId = creatorUserId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
        )
    }

    fun updateLocationPlan(
        planId: Long,
        userId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        val currentUrl = planService.getLocationUrlForUpdate(planId = planId, userId = userId)
        val normalizedUrl = url.trim()
        val provider = detectMapProvider(normalizedUrl)
        val unchangedUrl = currentUrl.takeIf { it == normalizedUrl }
        val resolution =
            if (unchangedUrl != null) {
                null
            } else {
                resolveLocation(normalizedUrl, provider)
            }

        return planService.updateLocationPlan(
            planId = planId,
            userId = userId,
            category = category,
            title = title,
            url = url,
            locationResolution = resolution,
            expectedUrlWhenResolutionSkipped = unchangedUrl,
        )
    }

    private fun detectMapProvider(url: String): MapProvider =
        planMapUrlDetector.detect(url) ?: throw BusinessException(PreLogErrorCode.INVALID_MAP_URL)

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
