package org.com.belog.prelog.infrastructure

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.service.result.PlanLocationResolution

interface PlanLocationResolver {
    val provider: MapProvider

    fun resolve(url: String): PlanLocationResolution
}
