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
                shortUrlResolver.resolve(uri) ?: return PlanLocationResolution.Failed
            } else {
                uri
            }

        val reference = urlParser.parse(resolvedUri) ?: return PlanLocationResolution.Failed
        val place =
            reference.placeId?.let(placesClient::getPlace)
                ?: reference.placeName?.let { placeName -> placesClient.searchPlace(placeName, reference.coordinates) }

        return place?.let(::resolvePlace)
            ?: reference.coordinates?.let { coordinates -> resolveCoordinates(coordinates, reference.placeName) }
            ?: PlanLocationResolution.Failed
    }

    private fun resolvePlace(place: GooglePlace): PlanLocationResolution =
        createResolvedLocation(
            externalPlaceId = place.id,
            placeName = place.name,
            address = place.address,
            latitude = place.latitude,
            longitude = place.longitude,
        )

    private fun resolveCoordinates(
        coordinates: GoogleMapCoordinates,
        placeName: String?,
    ): PlanLocationResolution =
        createResolvedLocation(
            externalPlaceId = null,
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
            PlanLocationResolution.Failed
        }
}
