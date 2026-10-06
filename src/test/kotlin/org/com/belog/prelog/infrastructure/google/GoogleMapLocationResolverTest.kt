package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.config.GoogleMapsProperties
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.service.PlanLocationResolution
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GoogleMapLocationResolverTest {
    @Test
    fun `Places 조회에 성공하면 장소 ID와 주소와 좌표를 사용한다`() {
        val restClientBuilder = RestClient.builder().baseUrl("https://places.googleapis.com")
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val placesClient = GooglePlacesClient(restClientBuilder.build(), properties("test-api-key"))
        val resolver = resolver(placesClient)
        server
            .expect(requestTo("https://places.googleapis.com/v1/places/test-place-id"))
            .andRespond(
                withSuccess(
                    """{"id":"test-place-id","displayName":{"text":"카페"},"formattedAddress":"서울특별시 종로구","location":{"latitude":37.57,"longitude":126.98}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result =
            assertIs<PlanLocationResolution.Resolved>(
                resolver.resolve("https://www.google.com/maps/search/?api=1&query=37.57,126.98&query_place_id=test-place-id"),
            )

        assertEquals(MapProvider.GOOGLE, result.location.provider)
        assertEquals("test-place-id", result.location.externalPlaceId)
        assertEquals("서울특별시 종로구", result.location.address)
        assertEquals(BigDecimal("37.5700000"), result.location.latitude)
        assertEquals(BigDecimal("126.9800000"), result.location.longitude)
        server.verify()
    }

    @Test
    fun `Places 조회가 불가능해도 URL 좌표가 있으면 좌표만 사용한다`() {
        val placesClient = GooglePlacesClient(RestClient.builder().build(), properties(""))
        val resolver = resolver(placesClient)

        val result =
            assertIs<PlanLocationResolution.Resolved>(
                resolver.resolve("https://www.google.com/maps/place/Test+Cafe/@37.57,126.98,17z"),
            )

        assertEquals(MapProvider.GOOGLE, result.location.provider)
        assertNull(result.location.externalPlaceId)
        assertNull(result.location.address)
        assertEquals(BigDecimal("37.5700000"), result.location.latitude)
        assertEquals(BigDecimal("126.9800000"), result.location.longitude)
    }

    private fun resolver(placesClient: GooglePlacesClient): GoogleMapLocationResolver =
        GoogleMapLocationResolver(
            urlDetector = GoogleMapUrlDetector(),
            shortUrlResolver = org.mockito.Mockito.mock(GoogleMapShortUrlResolver::class.java),
            urlParser = GoogleMapUrlParser(GooglePlusCodeDecoder()),
            placesClient = placesClient,
        )

    private fun properties(apiKey: String): GoogleMapsProperties =
        GoogleMapsProperties(
            apiKey = apiKey,
            connectionTimeout = Duration.ofSeconds(2),
            readTimeout = Duration.ofSeconds(3),
            maxRedirects = 3,
        )
}
