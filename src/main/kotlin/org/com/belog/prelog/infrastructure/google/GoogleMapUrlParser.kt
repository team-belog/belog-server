package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.domain.PlanLocation
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Component
class GoogleMapUrlParser(
    private val plusCodeDecoder: GooglePlusCodeDecoder,
) {
    fun parse(uri: URI): GoogleMapUrlReference? {
        val coordinates =
            coordinateQuery(uri)
                ?: parseEmbeddedCoordinates(uri.toString())
                ?: parsePathCoordinates(uri.path.orEmpty())
                ?: parsePlusCodeCoordinates(uri.toString())
        val placeId =
            queryParameter(uri, "query_place_id")
                ?.takeIf { it.isNotBlank() && it.length <= PlanLocation.EXTERNAL_PLACE_ID_MAX_LENGTH }
        val placeName = parsePlaceName(uri)

        if (placeId == null && placeName == null && coordinates == null) {
            return null
        }

        return GoogleMapUrlReference(
            placeId = placeId,
            placeName = placeName,
            coordinates = coordinates,
        )
    }

    private fun coordinateQuery(uri: URI): GoogleMapCoordinates? =
        COORDINATE_QUERY_NAMES
            .asSequence()
            .mapNotNull { name -> queryParameter(uri, name) }
            .mapNotNull(::parseCoordinates)
            .firstOrNull()

    private fun queryParameter(
        uri: URI,
        name: String,
    ): String? =
        uri.rawQuery
            ?.split("&")
            ?.asSequence()
            ?.map { parameter -> parameter.split("=", limit = 2) }
            ?.firstOrNull { parameter -> decode(parameter.first()) == name }
            ?.getOrNull(1)
            ?.let(::decode)

    private fun parseCoordinates(value: String): GoogleMapCoordinates? {
        val match = COORDINATE_QUERY_REGEX.matchEntire(value.trim()) ?: return null
        return coordinates(match.groupValues[1], match.groupValues[2])
    }

    private fun parseEmbeddedCoordinates(url: String): GoogleMapCoordinates? {
        val match = EMBEDDED_COORDINATE_REGEX.find(url) ?: return null
        return coordinates(match.groupValues[1], match.groupValues[2])
    }

    private fun parsePathCoordinates(path: String): GoogleMapCoordinates? {
        val match = PATH_COORDINATE_REGEX.find(path) ?: return null
        return coordinates(match.groupValues[1], match.groupValues[2])
    }

    private fun parsePlusCodeCoordinates(url: String): GoogleMapCoordinates? {
        val encodedPlusCode = PLUS_CODE_DATA_REGEX.find(url)?.groupValues?.get(1) ?: return null
        return plusCodeDecoder.decode(decode(encodedPlusCode))
    }

    private fun parsePlaceName(uri: URI): String? {
        val segments = uri.rawPath.orEmpty().split("/")
        val placeSegmentIndex = segments.indexOf("place")
        if (placeSegmentIndex < 0) {
            return null
        }

        return segments
            .getOrNull(placeSegmentIndex + 1)
            ?.let(::decode)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    private fun coordinates(
        latitude: String,
        longitude: String,
    ): GoogleMapCoordinates? =
        runCatching {
            GoogleMapCoordinates(
                latitude = BigDecimal(latitude),
                longitude = BigDecimal(longitude),
            )
        }.getOrNull()

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8) }.getOrDefault(value)

    companion object {
        private val COORDINATE_QUERY_NAMES = listOf("query", "q", "ll", "center")
        private val COORDINATE_QUERY_REGEX = Regex("^(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)$")
        private val EMBEDDED_COORDINATE_REGEX = Regex("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)")
        private val PATH_COORDINATE_REGEX = Regex("/@(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)")
        private val PLUS_CODE_DATA_REGEX = Regex("!20s([^!/?&#]+)")
    }
}

data class GoogleMapUrlReference(
    val placeId: String?,
    val placeName: String?,
    val coordinates: GoogleMapCoordinates?,
)

data class GoogleMapCoordinates(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)
