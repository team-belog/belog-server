package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.infrastructure.google.GoogleMapUrlDetector
import org.com.belog.prelog.infrastructure.kakao.KakaoMapUrlDetector
import org.com.belog.prelog.infrastructure.naver.NaverMapUrlDetector
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PlanLocationServiceTest {
    private val planService = mock(PlanService::class.java)
    private val mapUrlDetector =
        PlanMapUrlDetector(
            listOf(
                GoogleMapUrlDetector(),
                KakaoMapUrlDetector(),
                NaverMapUrlDetector(),
            ),
        )

    @Test
    fun `일반 URL로 위치 계획을 생성할 수 없다`() {
        val service = PlanLocationService(planService, mapUrlDetector, emptyList())

        val exception =
            assertFailsWith<BusinessException> {
                service.createLocationPlan(
                    meetingId = 1L,
                    creatorUserId = 15L,
                    category = PlanCategory.RESTAURANT,
                    title = "맛집",
                    url = "https://example.com/restaurant",
                )
            }

        assertEquals(PreLogErrorCode.INVALID_MAP_URL, exception.errorCode)
        verify(planService).validatePlanCreation(meetingId = 1L, userId = 15L)
        verifyNoMoreInteractions(planService)
    }

    @Test
    fun `일반 URL로 위치 계획을 수정할 수 없다`() {
        val service = PlanLocationService(planService, mapUrlDetector, emptyList())
        `when`(planService.getLocationUrlForUpdate(planId = 7L, userId = 15L))
            .thenReturn("https://example.com/restaurant")

        val exception =
            assertFailsWith<BusinessException> {
                service.updateLocationPlan(
                    planId = 7L,
                    userId = 15L,
                    category = PlanCategory.RESTAURANT,
                    title = "맛집",
                    url = "https://example.com/restaurant",
                )
            }

        assertEquals(PreLogErrorCode.INVALID_MAP_URL, exception.errorCode)
        verify(planService).getLocationUrlForUpdate(planId = 7L, userId = 15L)
        verifyNoMoreInteractions(planService)
    }

    @Test
    fun `지원하는 지도 URL의 위치 추출 실패는 실패 상태로 저장한다`() {
        val url = "https://www.google.com/maps/place/Test"
        val resolution = PlanLocationResolution.ProviderFailed(MapProvider.GOOGLE)
        val resolver = mock(PlanLocationResolver::class.java)
        val plan = mock(Plan::class.java)
        val service = PlanLocationService(planService, mapUrlDetector, listOf(resolver))
        `when`(resolver.provider).thenReturn(MapProvider.GOOGLE)
        `when`(resolver.resolve(url)).thenReturn(PlanLocationResolution.NotApplicable)
        `when`(
            planService.createLocationPlan(
                meetingId = 1L,
                creatorUserId = 15L,
                category = PlanCategory.RESTAURANT,
                title = "맛집",
                url = url,
                locationResolution = resolution,
            ),
        ).thenReturn(plan)

        val result =
            service.createLocationPlan(
                meetingId = 1L,
                creatorUserId = 15L,
                category = PlanCategory.RESTAURANT,
                title = "맛집",
                url = url,
            )

        assertSame(plan, result)
        verify(resolver).resolve(url)
    }
}
