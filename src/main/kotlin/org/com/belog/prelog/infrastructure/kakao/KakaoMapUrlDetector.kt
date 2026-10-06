package org.com.belog.prelog.infrastructure.kakao

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.service.MapUrlDetector
import org.springframework.stereotype.Component
import java.net.URI

@Component
class KakaoMapUrlDetector : MapUrlDetector {
    override val provider: MapProvider = MapProvider.KAKAO

    override fun supports(uri: URI): Boolean =
        uri.scheme?.lowercase() in SUPPORTED_SCHEMES && uri.host.equals(PLACE_HOST, ignoreCase = true)

    companion object {
        const val PLACE_HOST = "place.map.kakao.com"
        private val SUPPORTED_SCHEMES = setOf("http", "https")
    }
}
