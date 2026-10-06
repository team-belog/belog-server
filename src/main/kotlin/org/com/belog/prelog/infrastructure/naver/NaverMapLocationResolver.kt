package org.com.belog.prelog.infrastructure.naver

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.service.PlanLocationResolution
import org.com.belog.prelog.service.PlanLocationResolver
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class NaverMapLocationResolver(
    private val urlDetector: NaverMapUrlDetector,
) : PlanLocationResolver {
    override val provider: MapProvider = MapProvider.NAVER

    private val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    override fun resolve(url: String): PlanLocationResolution {
        val uri = runCatching { URI.create(url.trim()) }.getOrNull() ?: return PlanLocationResolution.NotApplicable
        if (urlDetector.isDirectMapUrl(uri)) {
            return failedResolution(uri)
        }
        if (!urlDetector.isShortUrl(uri)) {
            return PlanLocationResolution.NotApplicable
        }

        var currentUri = uri
        repeat(MAX_REDIRECTS) {
            val nextUri = redirectTarget(currentUri) ?: return PlanLocationResolution.NotApplicable
            if (urlDetector.isDirectMapUrl(nextUri)) {
                return failedResolution(nextUri)
            }
            if (!urlDetector.isShortUrl(nextUri)) {
                return PlanLocationResolution.NotApplicable
            }
            currentUri = nextUri
        }

        return PlanLocationResolution.NotApplicable
    }

    private fun failedResolution(uri: URI): PlanLocationResolution.ProviderFailed =
        PlanLocationResolution.ProviderFailed(
            provider = MapProvider.NAVER,
            externalPlaceId = PLACE_PATH_REGEX.matchEntire(uri.path.orEmpty())?.groupValues?.get(1),
        )

    private fun redirectTarget(uri: URI): URI? {
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(READ_TIMEOUT)
                .GET()
                .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() !in REDIRECT_STATUS_CODES) {
                return null
            }
            val location = response.headers().firstValue("Location").orElse(null) ?: return null
            runCatching { uri.resolve(location) }.getOrNull()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (exception: IOException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private val CONNECTION_TIMEOUT = Duration.ofSeconds(2)
        private val READ_TIMEOUT = Duration.ofSeconds(3)
        private const val MAX_REDIRECTS = 3
        private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        private val PLACE_PATH_REGEX = Regex("^/(?:p|v5)/entry/place/(\\d{1,512})/?$")
    }
}
