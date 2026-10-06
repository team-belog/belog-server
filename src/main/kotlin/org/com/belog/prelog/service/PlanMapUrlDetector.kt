package org.com.belog.prelog.service

import org.com.belog.prelog.domain.MapProvider
import org.springframework.stereotype.Component
import java.net.URI

@Component
class PlanMapUrlDetector(
    private val mapUrlDetectors: List<MapUrlDetector>,
) {
    fun detect(url: String): MapProvider? {
        val uri = runCatching { URI.create(url.trim()) }.getOrNull() ?: return null

        return mapUrlDetectors.firstOrNull { detector -> detector.supports(uri) }?.provider
    }
}
