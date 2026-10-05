package org.com.belog.prelog.service

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.PlanLocation

sealed interface PlanLocationResolution {
    data object NotApplicable : PlanLocationResolution

    data class Resolved(
        val location: PlanLocation,
    ) : PlanLocationResolution

    data object Failed : PlanLocationResolution

    data class ProviderFailed(
        val provider: MapProvider,
        val externalPlaceId: String? = null,
    ) : PlanLocationResolution
}
