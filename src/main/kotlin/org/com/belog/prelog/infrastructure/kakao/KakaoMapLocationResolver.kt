package org.com.belog.prelog.infrastructure.kakao

import org.com.belog.prelog.config.KakaoMapsConfig
import org.com.belog.prelog.config.KakaoMapsProperties
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.PlanLocation
import org.com.belog.prelog.service.PlanLocationResolution
import org.com.belog.prelog.service.PlanLocationResolver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.io.IOException
import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class KakaoMapLocationResolver(
    @Qualifier(KakaoMapsConfig.KAKAO_MAPS_HTTP_CLIENT) private val httpClient: HttpClient,
    private val properties: KakaoMapsProperties,
    private val urlDetector: KakaoMapUrlDetector,
) : PlanLocationResolver {
    override val provider: MapProvider = MapProvider.KAKAO

    override fun resolve(url: String): PlanLocationResolution {
        val uri = runCatching { URI.create(url.trim()) }.getOrNull() ?: return PlanLocationResolution.NotApplicable
        if (!urlDetector.supports(uri)) {
            return PlanLocationResolution.NotApplicable
        }

        val placeId =
            PLACE_PATH_REGEX.matchEntire(uri.path.orEmpty())?.groupValues?.get(1)
                ?: return PlanLocationResolution.ProviderFailed(MapProvider.KAKAO)
        val failed = PlanLocationResolution.ProviderFailed(MapProvider.KAKAO, placeId)
        val placeUri = URI.create("https://${KakaoMapUrlDetector.PLACE_HOST}/$placeId")
        val html = fetchPlacePage(placeUri) ?: return failed
        val metadata = parseMetadata(html)
        if (!isMatchingPlace(metadata["og:url"], placeId)) {
            return failed
        }

        val placeName = metadata["og:title"]?.let(HtmlUtils::htmlUnescape)?.takeIf(String::isNotBlank)
        val address = metadata["og:description"]?.let(HtmlUtils::htmlUnescape)?.takeIf(String::isNotBlank)
        val partial = failed.copy(placeName = placeName, address = address)
        val coordinates =
            parseCoordinates(metadata["twitter:image"]?.let(HtmlUtils::htmlUnescape))
                ?: return partial

        return try {
            PlanLocationResolution.Resolved(
                PlanLocation.create(
                    provider = MapProvider.KAKAO,
                    externalPlaceId = placeId,
                    placeName = placeName,
                    address = address,
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                ),
            )
        } catch (exception: IllegalArgumentException) {
            partial
        }
    }

    private fun fetchPlacePage(uri: URI): String? {
        val request =
            HttpRequest
                .newBuilder(uri)
                .timeout(properties.readTimeout)
                .GET()
                .build()
        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            response.body().use { body ->
                if (response.statusCode() != 200 ||
                    response
                        .headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .substringBefore(";")
                        .trim() != "text/html"
                ) {
                    return null
                }

                val bytes = body.readNBytes(MAX_HTML_BYTES + 1)
                if (bytes.size > MAX_HTML_BYTES) {
                    return null
                }
                String(bytes, StandardCharsets.UTF_8)
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (exception: IOException) {
            null
        }
    }

    private fun parseMetadata(html: String): Map<String, String> =
        META_TAG_REGEX
            .findAll(html)
            .mapNotNull { match ->
                val attributes =
                    ATTRIBUTE_REGEX
                        .findAll(match.value)
                        .associate { attribute ->
                            attribute.groupValues[1].lowercase() to
                                (attribute.groupValues[2].ifEmpty { attribute.groupValues[3] })
                        }
                val name = attributes["property"] ?: attributes["name"]
                val content = attributes["content"]
                if (name == null || content == null) null else name.lowercase() to content
            }.toMap()

    private fun isMatchingPlace(
        canonicalUrl: String?,
        expectedPlaceId: String,
    ): Boolean {
        val uri = runCatching { URI.create(canonicalUrl ?: return false) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(KakaoMapUrlDetector.PLACE_HOST, ignoreCase = true) &&
            PLACE_PATH_REGEX.matchEntire(uri.path.orEmpty())?.groupValues?.get(1) == expectedPlaceId
    }

    private fun parseCoordinates(imageUrl: String?): Pair<BigDecimal, BigDecimal>? {
        val uri = runCatching { URI.create(imageUrl ?: return null) }.getOrNull() ?: return null
        if (uri.host?.lowercase() != STATIC_MAP_HOST || uri.path != STATIC_MAP_PATH) {
            return null
        }

        val parameters =
            uri.rawQuery
                ?.split("&")
                ?.mapNotNull { parameter ->
                    val parts = parameter.split("=", limit = 2)
                    parts.getOrNull(1)?.let { value -> parts[0] to URLDecoder.decode(value, StandardCharsets.UTF_8) }
                }?.toMap()
                ?: return null
        if (parameters["srs"] != "wgs84" || parameters["type"] != "place") {
            return null
        }

        val match = COORDINATES_REGEX.matchEntire(parameters["m"] ?: return null) ?: return null
        return runCatching { BigDecimal(match.groupValues[2]) to BigDecimal(match.groupValues[1]) }.getOrNull()
    }

    companion object {
        private const val STATIC_MAP_HOST = "staticmap.kakao.com"
        private const val STATIC_MAP_PATH = "/staticmap/og"
        private const val MAX_HTML_BYTES = 65_536
        private val PLACE_PATH_REGEX = Regex("^/([1-9]\\d{0,19})/?$")
        private val META_TAG_REGEX = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val ATTRIBUTE_REGEX = Regex("([\\w:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")
        private val COORDINATES_REGEX = Regex("^(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)$")
    }
}
