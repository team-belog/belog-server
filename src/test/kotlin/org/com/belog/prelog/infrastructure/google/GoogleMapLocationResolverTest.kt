package org.com.belog.prelog.infrastructure.google

import org.com.belog.prelog.config.GoogleMapsProperties
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.PlanLocation
import org.com.belog.prelog.service.result.PlanLocationResolution
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
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
    fun `모바일 공유 링크의 q 파라미터가 좌표가 아니면 장소명으로 검색한다`() {
        val restClientBuilder = RestClient.builder().baseUrl("https://places.googleapis.com")
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val placesClient = GooglePlacesClient(restClientBuilder.build(), properties("test-api-key"))
        val resolver = resolver(placesClient)
        server
            .expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
            .andExpect(jsonPath("$.textQuery").value("서울특별시 서초구 반포동 734-26 복태포차 1층"))
            .andRespond(
                withSuccess(
                    """{"places":[{"id":"test-place-id","displayName":{"text":"복태포차"},"formattedAddress":"서울특별시 서초구 반포동 734-26","location":{"latitude":37.50,"longitude":127.00}}]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result =
            assertIs<PlanLocationResolution.Resolved>(
                resolver.resolve(
                    "https://maps.google.com?q=%EC%84%9C%EC%9A%B8%ED%8A%B9%EB%B3%84%EC%8B%9C+%EC%84%9C%EC%B4%88%EA%B5%AC+" +
                        "%EB%B0%98%ED%8F%AC%EB%8F%99+734-26+%EB%B3%B5%ED%83%9C%ED%8F%AC%EC%B0%A8+1%EC%B8%B5" +
                        "&ftid=0x357ca3e104d973b1:0x53f2689f86672e2f&entry=gps&g_st=ic",
                ),
            )

        assertEquals("test-place-id", result.location.externalPlaceId)
        assertEquals("복태포차", result.location.placeName)
        assertEquals(BigDecimal("37.5000000"), result.location.latitude)
        assertEquals(BigDecimal("127.0000000"), result.location.longitude)
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

    @Test
    fun `장소명 제한을 넘는 q가 있어도 Places 조회가 불가능하면 URL 좌표로 해결한다`() {
        val placesClient = GooglePlacesClient(RestClient.builder().build(), properties(""))
        val resolver = resolver(placesClient)
        val longQuery = "가".repeat(PlanLocation.PLACE_NAME_MAX_LENGTH + 1)

        val result =
            assertIs<PlanLocationResolution.Resolved>(
                resolver.resolve("https://maps.google.com/?q=$longQuery&ll=37.57,126.98"),
            )

        assertNull(result.location.placeName)
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
