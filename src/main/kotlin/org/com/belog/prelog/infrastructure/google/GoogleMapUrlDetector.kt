package org.com.belog.prelog.infrastructure.google

import org.springframework.stereotype.Component
import java.net.URI

@Component
class GoogleMapUrlDetector {
    fun isGoogleMapUrl(uri: URI): Boolean = isShortUrl(uri) || isDirectMapUrl(uri)

    fun isShortUrl(uri: URI): Boolean {
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return false
        }

        return when (uri.host?.lowercase()) {
            "maps.app.goo.gl" -> true
            "goo.gl" -> uri.path?.startsWith("/maps") == true
            else -> false
        }
    }

    fun isDirectMapUrl(uri: URI): Boolean {
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return false
        }

        val host = uri.host?.lowercase() ?: return false
        return when {
            host == "maps.google.com" -> true
            isGoogleHost(host) -> uri.path?.startsWith("/maps") == true
            else -> false
        }
    }

    private fun isGoogleHost(host: String): Boolean =
        host == "google.com" ||
            host.endsWith(".google.com") ||
            host == "google.co.kr" ||
            host.endsWith(".google.co.kr")
}
