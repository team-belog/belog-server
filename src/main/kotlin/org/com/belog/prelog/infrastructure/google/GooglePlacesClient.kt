package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.config.GoogleMapsConfig
import org.com.belog.prelog.config.GoogleMapsProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal

@Component
class GooglePlacesClient(
    @Qualifier(GoogleMapsConfig.GOOGLE_PLACES_REST_CLIENT) private val restClient: RestClient,
    private val properties: GoogleMapsProperties,
) {
    fun getPlace(placeId: String): GooglePlace? {
        if (properties.apiKey.isBlank()) {
            return null
        }

        return try {
            restClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                        .pathSegment("v1", "places", placeId)
                        .build()
                }.header("X-Goog-Api-Key", properties.apiKey)
                .header("X-Goog-FieldMask", "id,displayName,formattedAddress,location")
                .retrieve()
                .body(GooglePlaceResponse::class.java)
                ?.toGooglePlace()
        } catch (exception: RestClientException) {
            null
        }
    }

    fun searchPlace(
        placeName: String,
        coordinates: GoogleMapCoordinates?,
    ): GooglePlace? {
        if (properties.apiKey.isBlank()) {
            return null
        }

        val request =
            GooglePlaceTextSearchRequest(
                textQuery = placeName,
                languageCode = "ko",
                regionCode = "KR",
                maxResultCount = 1,
                locationBias = coordinates?.toLocationBias(),
            )

        return try {
            restClient
                .post()
                .uri("/v1/places:searchText")
                .header("X-Goog-Api-Key", properties.apiKey)
                .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location")
                .body(request)
                .retrieve()
                .body(GooglePlaceTextSearchResponse::class.java)
                ?.places
                ?.firstOrNull()
                ?.toGooglePlace()
        } catch (exception: RestClientException) {
            null
        }
    }

    private fun GooglePlaceResponse.toGooglePlace(): GooglePlace? {
        val responseLocation = location ?: return null
        return GooglePlace(
            id = id,
            name = displayName?.text,
            address = formattedAddress,
            latitude = responseLocation.latitude,
            longitude = responseLocation.longitude,
        )
    }

    private fun GoogleMapCoordinates.toLocationBias(): GooglePlaceLocationBias =
        GooglePlaceLocationBias(
            circle =
                GooglePlaceCircle(
                    center =
                        GooglePlaceLocationResponse(
                            latitude = latitude,
                            longitude = longitude,
                        ),
                    radius = LOCATION_BIAS_RADIUS_METERS,
                ),
        )

    companion object {
        private const val LOCATION_BIAS_RADIUS_METERS = 100.0
    }
}

data class GooglePlace(
    val id: String,
    val name: String?,
    val address: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)

private data class GooglePlaceResponse(
    val id: String,
    val displayName: GooglePlaceDisplayNameResponse? = null,
    val formattedAddress: String? = null,
    val location: GooglePlaceLocationResponse? = null,
)

private data class GooglePlaceDisplayNameResponse(
    val text: String,
)

private data class GooglePlaceLocationResponse(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
)

private data class GooglePlaceTextSearchRequest(
    val textQuery: String,
    val languageCode: String,
    val regionCode: String,
    val maxResultCount: Int,
    val locationBias: GooglePlaceLocationBias?,
)

private data class GooglePlaceLocationBias(
    val circle: GooglePlaceCircle,
)

private data class GooglePlaceCircle(
    val center: GooglePlaceLocationResponse,
    val radius: Double,
)

private data class GooglePlaceTextSearchResponse(
    val places: List<GooglePlaceResponse> = emptyList(),
)
