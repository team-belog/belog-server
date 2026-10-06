package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.LocationResolutionStatus
import org.com.belog.prelog.domain.MapProvider
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.infrastructure.google.GoogleMapLocationResolver
import org.com.belog.prelog.infrastructure.google.GoogleMapShortUrlResolver
import org.com.belog.prelog.infrastructure.google.GoogleMapUrlDetector
import org.com.belog.prelog.infrastructure.google.GoogleMapUrlParser
import org.com.belog.prelog.infrastructure.google.GooglePlacesClient
import org.com.belog.prelog.infrastructure.google.GooglePlusCodeDecoder
import org.com.belog.prelog.infrastructure.kakao.KakaoMapUrlDetector
import org.com.belog.prelog.infrastructure.naver.NaverMapUrlDetector
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlanServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val planLikeRepository = mock(PlanLikeRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC)
    private val planService = PlanService(meetingRepository, groupMemberRepository, planRepository, planLikeRepository, clock)

    @Test
    fun `일반 URL 계획은 위치 처리 없이 저장한다`() {
        val context = meetingContext()
        val creator = mock(GroupMember::class.java)
        `when`(creator.group).thenReturn(context.group)
        `when`(creator.belongsTo(context.group)).thenCallRealMethod()
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(creator)
        `when`(planRepository.save(any(Plan::class.java))).thenAnswer { invocation -> invocation.getArgument(0) }
        val resolver =
            GoogleMapLocationResolver(
                GoogleMapUrlDetector(),
                mock(GoogleMapShortUrlResolver::class.java),
                GoogleMapUrlParser(GooglePlusCodeDecoder()),
                mock(GooglePlacesClient::class.java),
            )

        val plan =
            PlanLinkService(
                planService,
                PlanMapUrlDetector(listOf(GoogleMapUrlDetector(), KakaoMapUrlDetector(), NaverMapUrlDetector())),
                listOf(resolver),
            ).createLinkPlan(1L, 15L, PlanCategory.RESTAURANT, "맛집", "https://example.com/restaurant")

        assertEquals(LocationResolutionStatus.NOT_APPLICABLE, plan.locationStatus)
        assertNull(plan.location)
        assertEquals("https://example.com/restaurant", plan.url)
    }

    @Test
    fun `그룹 멤버만 계획 목록을 조회할 수 있다`() {
        val context = meetingContext()
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                planService.getPlans(
                    meetingId = 1L,
                    userId = 15L,
                    category = null,
                    pinnedOnly = false,
                    cursor = null,
                )
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        verifyNoInteractions(planRepository)
    }

    @Test
    fun `계획 작성자는 계획을 삭제할 수 있다`() {
        stubPlanList(loginGroupMemberId = 10L, planCreatorId = 10L, isMeetingCreator = false)

        val result = getPlans()

        assertTrue(result.items.single().canDelete)
    }

    @Test
    fun `만남 생성자는 다른 사용자의 계획도 삭제할 수 있다`() {
        stubPlanList(loginGroupMemberId = 10L, planCreatorId = 11L, isMeetingCreator = true)

        val result = getPlans()

        assertTrue(result.items.single().canDelete)
    }

    @Test
    fun `만남 생성자가 아닌 그룹 생성자는 다른 사용자의 계획을 삭제할 수 없다`() {
        stubPlanList(
            loginGroupMemberId = 10L,
            planCreatorId = 11L,
            isMeetingCreator = false,
            loginRole = GroupRole.OWNER,
        )

        val result = getPlans()

        assertFalse(result.items.single().canDelete)
    }

    @Test
    fun `계획 작성자는 자신의 계획을 수정할 수 있다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = true)

        val result =
            planService.updateLinkPlan(
                planId = 20L,
                userId = 15L,
                category = PlanCategory.CAFE,
                title = "수정한 카페",
                url = "https://example.com/updated-cafe",
            )

        assertSame(target.plan, result)
        assertEquals(PlanType.LINK, result.type)
        assertEquals(PlanCategory.CAFE, result.category)
        assertEquals("수정한 카페", result.title)
        assertEquals("https://example.com/updated-cafe", result.url)
    }

    @Test
    fun `위치 해석을 생략한 사이 URL이 변경되면 위치 정보를 보존하고 충돌을 반환한다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = true)
        target.plan.updateLink(PlanCategory.RESTAURANT, "다른 요청의 맛집", "https://maps.google.com/new-place")
        target.plan.failLocationResolution(MapProvider.GOOGLE, externalPlaceId = "new-place-id")

        val exception =
            assertFailsWith<BusinessException> {
                planService.updateLinkPlan(
                    planId = 20L,
                    userId = 15L,
                    category = PlanCategory.CAFE,
                    title = "수정한 카페",
                    url = "https://example.com/place",
                    expectedUrlWhenResolutionSkipped = "https://example.com/place",
                )
            }

        assertEquals(PreLogErrorCode.PLAN_UPDATE_CONFLICT, exception.errorCode)
        assertEquals("https://maps.google.com/new-place", target.plan.url)
        assertEquals(LocationResolutionStatus.FAILED, target.plan.locationStatus)
        assertEquals(MapProvider.GOOGLE, target.plan.location?.provider)
        assertEquals("new-place-id", target.plan.location?.externalPlaceId)
    }

    @Test
    fun `계획 작성자가 아니면 계획을 수정할 수 없다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = false)

        val exception =
            assertFailsWith<BusinessException> {
                planService.updateMemoPlan(
                    planId = 20L,
                    userId = 15L,
                    category = PlanCategory.OTHER,
                    title = "수정 시도",
                    content = "수정할 수 없는 내용",
                )
            }

        assertEquals(PreLogErrorCode.PLAN_UPDATE_FORBIDDEN, exception.errorCode)
        assertEquals(PlanType.LINK, target.plan.type)
        assertEquals("맛집", target.plan.title)
        assertEquals("https://example.com/place", target.plan.url)
    }

    @Test
    fun `서비스에서 계획 작성자는 자신의 계획을 삭제할 수 있다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = true)

        planService.deletePlan(planId = 20L, userId = 15L)

        verify(planLikeRepository).deleteAllByPlanId(20L)
        verify(planRepository).delete(target.plan)
    }

    @Test
    fun `서비스에서 만남 생성자는 다른 사용자의 계획을 삭제할 수 있다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = false, loginIsMeetingCreator = true)

        planService.deletePlan(planId = 20L, userId = 15L)

        verify(planLikeRepository).deleteAllByPlanId(20L)
        verify(planRepository).delete(target.plan)
    }

    @Test
    fun `작성자와 만남 생성자가 아니면 계획을 삭제할 수 없다`() {
        val target = stubPlanActionTarget(loginIsPlanCreator = false, loginIsMeetingCreator = false)

        val exception =
            assertFailsWith<BusinessException> {
                planService.deletePlan(planId = 20L, userId = 15L)
            }

        assertEquals(PreLogErrorCode.PLAN_DELETE_FORBIDDEN, exception.errorCode)
        verifyNoInteractions(planLikeRepository)
        verify(planRepository, never()).delete(target.plan)
    }

    @Test
    fun `존재하지 않는 계획은 수정하거나 삭제할 수 없다`() {
        `when`(planRepository.findByIdWithMeetingAndCreator(20L)).thenReturn(null)

        val updateException =
            assertFailsWith<BusinessException> {
                planService.updateLinkPlan(
                    planId = 20L,
                    userId = 15L,
                    category = PlanCategory.CAFE,
                    title = "카페",
                    url = "https://example.com/cafe",
                )
            }
        val deleteException =
            assertFailsWith<BusinessException> {
                planService.deletePlan(planId = 20L, userId = 15L)
            }

        assertEquals(PreLogErrorCode.PLAN_NOT_FOUND, updateException.errorCode)
        assertEquals(PreLogErrorCode.PLAN_NOT_FOUND, deleteException.errorCode)
        verifyNoInteractions(planLikeRepository)
    }

    @Test
    fun `일반 그룹 멤버는 다른 사용자의 계획을 핀 고정할 수 있다`() {
        val plan = stubPlanPinTarget()

        planService.pinPlan(planId = 20L, userId = 15L)

        assertTrue(plan.pinned)
    }

    @Test
    fun `일반 그룹 멤버는 다른 사용자가 고정한 계획을 해제할 수 있다`() {
        val plan = stubPlanPinTarget(initiallyPinned = true)

        planService.unpinPlan(planId = 20L, userId = 15L)

        assertFalse(plan.pinned)
    }

    @Test
    fun `그룹 멤버가 아니면 계획을 핀 고정하거나 해제할 수 없다`() {
        val context = meetingContext()
        val plan = mock(Plan::class.java)
        `when`(plan.meeting).thenReturn(context.meeting)
        `when`(planRepository.findByIdWithMeetingAndCreator(20L)).thenReturn(plan)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(null)

        val pinException =
            assertFailsWith<BusinessException> {
                planService.pinPlan(planId = 20L, userId = 15L)
            }
        val unpinException =
            assertFailsWith<BusinessException> {
                planService.unpinPlan(planId = 20L, userId = 15L)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, pinException.errorCode)
        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, unpinException.errorCode)
        verifyNoInteractions(planLikeRepository)
    }

    @Test
    fun `존재하지 않는 만남에는 계획을 생성할 수 없다`() {
        `when`(meetingRepository.findActiveById(1L)).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                planService.createLinkPlan(
                    meetingId = 1L,
                    creatorUserId = 15L,
                    category = PlanCategory.RESTAURANT,
                    title = "맛집",
                    url = "https://example.com/place",
                )
            }

        assertEquals(MeetingErrorCode.MEETING_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(planRepository)
    }

    @Test
    fun `해당 만남이 속한 그룹의 멤버가 아니면 계획을 생성할 수 없다`() {
        val context = meetingContext()
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                planService.createMemoPlan(
                    meetingId = 1L,
                    creatorUserId = 15L,
                    category = PlanCategory.OTHER,
                    title = "메모",
                    content = "내용",
                )
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        verifyNoInteractions(planRepository)
    }

    @Test
    fun `약속에 참여하지 않은 그룹 멤버도 계획을 생성할 수 있다`() {
        val context = meetingContext()
        val groupMember = mock(GroupMember::class.java)
        `when`(groupMember.group).thenReturn(context.group)
        `when`(groupMember.belongsTo(context.group)).thenCallRealMethod()
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(groupMember)
        `when`(planRepository.save(any(Plan::class.java))).thenAnswer { invocation -> invocation.getArgument(0) }

        val plan =
            planService.createMemoPlan(
                meetingId = 1L,
                creatorUserId = 15L,
                category = PlanCategory.OTHER,
                title = "메모",
                content = "내용",
            )

        assertSame(context.meeting, plan.meeting)
        assertSame(groupMember, plan.createdBy)
    }

    @Test
    fun `종료된 만남에는 계획을 생성할 수 없다`() {
        val context = meetingContext(endDate = LocalDate.of(2026, 9, 21))
        val groupMember = mock(GroupMember::class.java)
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(groupMember)

        val exception =
            assertFailsWith<BusinessException> {
                planService.createLinkPlan(
                    meetingId = 1L,
                    creatorUserId = 15L,
                    category = PlanCategory.CAFE,
                    title = "카페",
                    url = "https://example.com/cafe",
                )
            }

        assertEquals(PreLogErrorCode.MEETING_ALREADY_ENDED, exception.errorCode)
        verify(context.meeting).isEnded(LocalDate.of(2026, 9, 22))
        verifyNoInteractions(planRepository)
    }

    private fun meetingContext(endDate: LocalDate = LocalDate.of(2026, 9, 23)): MeetingContext {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        val currentDate = LocalDate.of(2026, 9, 22)
        `when`(group.id).thenReturn(3L)
        `when`(meeting.group).thenReturn(group)
        `when`(meeting.endDate).thenReturn(endDate)
        `when`(meeting.isEnded(currentDate)).thenReturn(endDate.isBefore(currentDate))
        return MeetingContext(group, meeting)
    }

    private fun stubPlanList(
        loginGroupMemberId: Long,
        planCreatorId: Long,
        isMeetingCreator: Boolean,
        loginRole: GroupRole = GroupRole.MEMBER,
    ) {
        val context = meetingContext()
        val loginGroupMember = mock(GroupMember::class.java)
        val planCreator = mock(GroupMember::class.java)
        val plan = mock(Plan::class.java)
        `when`(loginGroupMember.id).thenReturn(loginGroupMemberId)
        `when`(loginGroupMember.role).thenReturn(loginRole)
        `when`(planCreator.id).thenReturn(planCreatorId)
        `when`(plan.id).thenReturn(20L)
        `when`(plan.createdBy).thenReturn(planCreator)
        `when`(plan.type).thenReturn(PlanType.LINK)
        `when`(plan.category).thenReturn(PlanCategory.RESTAURANT)
        `when`(plan.title).thenReturn("맛집")
        `when`(plan.url).thenReturn("https://example.com/place")
        `when`(plan.createdAt).thenReturn(Instant.parse("2026-09-22T10:30:00Z"))
        `when`(meetingRepository.findActiveById(1L)).thenReturn(context.meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(loginGroupMember)
        `when`(context.meeting.isCreatedBy(loginGroupMember)).thenReturn(isMeetingCreator)
        `when`(
            planRepository.findPageWithCreator(
                meetingId = 1L,
                category = null,
                pinnedOnly = false,
                cursorPinned = null,
                cursorId = null,
                pageable = PageRequest.of(0, 21),
            ),
        ).thenReturn(listOf(plan))
    }

    private fun stubPlanPinTarget(initiallyPinned: Boolean = false): Plan {
        val context = meetingContext()
        val loginGroupMember = mock(GroupMember::class.java)
        val planCreator = mock(GroupMember::class.java)
        `when`(loginGroupMember.role).thenReturn(GroupRole.MEMBER)
        `when`(planCreator.group).thenReturn(context.group)
        `when`(planCreator.belongsTo(context.group)).thenCallRealMethod()

        val plan =
            Plan.createLink(
                meeting = context.meeting,
                creator = planCreator,
                category = PlanCategory.RESTAURANT,
                title = "맛집",
                url = "https://example.com/place",
                currentDate = LocalDate.of(2026, 9, 22),
            )
        ReflectionTestUtils.setField(plan, "id", 20L)
        if (initiallyPinned) {
            plan.pin()
        }

        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(loginGroupMember)
        `when`(planRepository.findByIdWithMeetingAndCreator(20L)).thenReturn(plan)

        return plan
    }

    private fun stubPlanActionTarget(
        loginIsPlanCreator: Boolean,
        loginIsMeetingCreator: Boolean = false,
    ): PlanActionTarget {
        val context = meetingContext()
        val planCreator = mock(GroupMember::class.java)
        val loginGroupMember = if (loginIsPlanCreator) planCreator else mock(GroupMember::class.java)
        `when`(planCreator.id).thenReturn(10L)
        `when`(planCreator.group).thenReturn(context.group)
        `when`(planCreator.belongsTo(context.group)).thenCallRealMethod()
        if (!loginIsPlanCreator) {
            `when`(loginGroupMember.id).thenReturn(11L)
        }

        val plan =
            Plan.createLink(
                meeting = context.meeting,
                creator = planCreator,
                category = PlanCategory.RESTAURANT,
                title = "맛집",
                url = "https://example.com/place",
                currentDate = LocalDate.of(2026, 9, 22),
            )
        ReflectionTestUtils.setField(plan, "id", 20L)

        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(loginGroupMember)
        `when`(planRepository.findByIdWithMeetingAndCreator(20L)).thenReturn(plan)
        `when`(context.meeting.isCreatedBy(loginGroupMember)).thenReturn(loginIsMeetingCreator)

        return PlanActionTarget(plan)
    }

    private fun getPlans() =
        planService.getPlans(
            meetingId = 1L,
            userId = 15L,
            category = null,
            pinnedOnly = false,
            cursor = null,
        )

    private data class MeetingContext(
        val group: Group,
        val meeting: Meeting,
    )

    private data class PlanActionTarget(
        val plan: Plan,
    )
}
