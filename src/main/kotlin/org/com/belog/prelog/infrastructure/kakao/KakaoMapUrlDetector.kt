package org.com.belog.prelog.infrastructure.kakao

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.infrastructure.MapUrlDetector
import org.springframework.stereotype.Component
import java.net.URI

@Component
class KakaoMapUrlDetector : MapUrlDetector {
    override val provider: MapProvider = MapProvider.KAKAO

    override fun supports(uri: URI): Boolean = isPlaceUrl(uri) || isShortUrl(uri)

    fun isPlaceUrl(uri: URI): Boolean = uri.scheme?.lowercase() in SUPPORTED_SCHEMES && uri.host.equals(PLACE_HOST, ignoreCase = true)

    fun isAppLinkUrl(uri: URI): Boolean = uri.scheme.equals("https", ignoreCase = true) && uri.host.equals(APP_LINK_HOST, ignoreCase = true)

    fun isShortUrl(uri: URI): Boolean =
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(SHORT_URL_HOST, ignoreCase = true) &&
            !uri.path.isNullOrBlank() &&
            uri.path != "/"

    companion object {
        const val PLACE_HOST = "place.map.kakao.com"
        private const val APP_LINK_HOST = "applink.map.kakao.com"
        private const val SHORT_URL_HOST = "kko.to"
        private val SUPPORTED_SCHEMES = setOf("http", "https")
    }
}
