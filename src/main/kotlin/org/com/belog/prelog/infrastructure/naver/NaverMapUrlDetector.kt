package org.com.belog.prelog.infrastructure.naver

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.service.MapUrlDetector
import org.springframework.stereotype.Component
import java.net.URI

@Component
class NaverMapUrlDetector : MapUrlDetector {
    override val provider: MapProvider = MapProvider.NAVER

    override fun supports(uri: URI): Boolean = isDirectMapUrl(uri) || isShortUrl(uri)

    fun isDirectMapUrl(uri: URI): Boolean =
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host?.lowercase() in DIRECT_MAP_HOSTS

    fun isShortUrl(uri: URI): Boolean =
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("naver.me", ignoreCase = true) &&
            !uri.path.isNullOrBlank() &&
            uri.path != "/"

    companion object {
        private val DIRECT_MAP_HOSTS = setOf("map.naver.com", "m.map.naver.com")
    }
}
