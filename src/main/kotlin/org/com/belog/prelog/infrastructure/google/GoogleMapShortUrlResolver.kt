package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.config.GoogleMapsConfig
import org.com.belog.prelog.config.GoogleMapsProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class GoogleMapShortUrlResolver(
    @Qualifier(GoogleMapsConfig.GOOGLE_MAPS_HTTP_CLIENT) private val httpClient: HttpClient,
    private val properties: GoogleMapsProperties,
    private val urlDetector: GoogleMapUrlDetector,
) {
    fun resolve(uri: URI): URI? {
        var currentUri = uri

        repeat(properties.maxRedirects) {
            if (urlDetector.isDirectMapUrl(currentUri)) {
                return currentUri
            }
            if (!urlDetector.isShortUrl(currentUri)) {
                return null
            }

            val response = send(currentUri) ?: return null
            if (response.statusCode() !in REDIRECT_STATUS_CODES) {
                return null
            }

            val location = response.headers().firstValue("Location").orElse(null) ?: return null
            currentUri = runCatching { currentUri.resolve(location) }.getOrNull() ?: return null
        }

        return currentUri.takeIf(urlDetector::isDirectMapUrl)
    }

    private fun send(uri: URI): HttpResponse<Void>? {
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(properties.readTimeout)
                .GET()
                .build()

        return try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
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
        private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
    }
}
