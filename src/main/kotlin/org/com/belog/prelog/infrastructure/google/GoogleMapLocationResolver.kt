package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.PlanLocation
import org.com.belog.prelog.service.PlanLocationResolution
import org.com.belog.prelog.service.PlanLocationResolver
import org.springframework.stereotype.Component
import java.net.URI

@Component
class GoogleMapLocationResolver(
    private val urlDetector: GoogleMapUrlDetector,
    private val shortUrlResolver: GoogleMapShortUrlResolver,
    private val urlParser: GoogleMapUrlParser,
    private val placesClient: GooglePlacesClient,
) : PlanLocationResolver {
    override fun resolve(url: String): PlanLocationResolution {
        val uri = runCatching { URI.create(url.trim()) }.getOrNull() ?: return PlanLocationResolution.NotApplicable
        if (!urlDetector.isGoogleMapUrl(uri)) {
            return PlanLocationResolution.NotApplicable
        }

        val resolvedUri =
            if (urlDetector.isShortUrl(uri)) {
                shortUrlResolver.resolve(uri) ?: return PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE)
            } else {
                uri
            }

        val reference = urlParser.parse(resolvedUri) ?: return PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE)
        val place =
            if (reference.placeId != null) {
                placesClient.getPlace(reference.placeId)
            } else {
                reference.placeName?.let { placeName -> placesClient.searchPlace(placeName, reference.coordinates) }
            }

        if (place != null) {
            return resolvePlace(place, reference.placeId, reference.placeName, reference.coordinates)
        }

        return reference.coordinates?.let { coordinates -> resolveCoordinates(coordinates, reference.placeId, reference.placeName) }
            ?: PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE, reference.placeId, reference.placeName)
    }

    private fun resolvePlace(
        place: GooglePlace,
        urlPlaceId: String?,
        urlPlaceName: String?,
        urlCoordinates: GoogleMapCoordinates?,
    ): PlanLocationResolution {
        val placeId = place.id ?: urlPlaceId
        val placeName = place.name ?: urlPlaceName
        val latitude = place.latitude ?: urlCoordinates?.latitude
        val longitude = place.longitude ?: urlCoordinates?.longitude
        if (latitude == null || longitude == null) {
            return PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE, placeId, placeName, place.address)
        }
        return createResolvedLocation(placeId, placeName, place.address, latitude, longitude)
    }

    private fun resolveCoordinates(
        coordinates: GoogleMapCoordinates,
        placeId: String?,
        placeName: String?,
    ): PlanLocationResolution =
        createResolvedLocation(
            externalPlaceId = placeId,
            placeName = placeName,
            address = null,
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
        )

    private fun createResolvedLocation(
        externalPlaceId: String?,
        placeName: String?,
        address: String?,
        latitude: java.math.BigDecimal,
        longitude: java.math.BigDecimal,
    ): PlanLocationResolution =
        try {
            PlanLocationResolution.Resolved(
                PlanLocation.create(
                    provider = MapProvider.GOOGLE,
                    externalPlaceId = externalPlaceId,
                    placeName = placeName,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
        } catch (exception: IllegalArgumentException) {
            PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE, externalPlaceId, placeName, address)
        }
}
