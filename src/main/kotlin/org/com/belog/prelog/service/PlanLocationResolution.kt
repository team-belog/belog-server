package org.com.belog.prelog.service

import org.com.belog.prelog.domain.PlanLocation

sealed interface PlanLocationResolution {
    data object NotApplicable : PlanLocationResolution

    data class Resolved(
        val location: PlanLocation,
    ) : PlanLocationResolution

    data object Failed : PlanLocationResolution
}
