package org.com.belog.prelog.service

import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.infrastructure.google.GoogleMapUrlDetector
import org.com.belog.prelog.infrastructure.kakao.KakaoMapUrlDetector
import org.com.belog.prelog.infrastructure.naver.NaverMapUrlDetector
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanMapUrlDetectorTest {
    private val detector =
        PlanMapUrlDetector(
            listOf(
                GoogleMapUrlDetector(),
                KakaoMapUrlDetector(),
                NaverMapUrlDetector(),
            ),
        )

    @Test
    fun `Google 지도 URL의 제공자를 판별한다`() {
        assertEquals(
            MapProvider.GOOGLE,
            detector.detect("https://www.google.com/maps/place/Test/@37.57,126.98,17z"),
        )
        assertEquals(MapProvider.GOOGLE, detector.detect("https://maps.app.goo.gl/example"))
    }

    @Test
    fun `Kakao 지도 URL의 제공자를 판별한다`() {
        assertEquals(MapProvider.KAKAO, detector.detect("https://place.map.kakao.com/123456"))
    }

    @Test
    fun `Naver 지도 URL의 제공자를 판별한다`() {
        assertEquals(MapProvider.NAVER, detector.detect("https://map.naver.com/p/entry/place/123456"))
        assertEquals(MapProvider.NAVER, detector.detect("https://naver.me/example"))
    }

    @Test
    fun `일반 URL과 잘못된 URL은 지도 제공자로 판별하지 않는다`() {
        assertNull(detector.detect("https://example.com/restaurant"))
        assertNull(detector.detect("https://maps.google.com.evil.example/maps/place/test"))
        assertNull(detector.detect("not a url"))
    }
}
