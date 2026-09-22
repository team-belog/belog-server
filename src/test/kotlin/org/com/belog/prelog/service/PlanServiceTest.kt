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
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.repository.PlanRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlanServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC)
    private val planService = PlanService(meetingRepository, groupMemberRepository, planRepository, clock)

    @Test
    fun `그룹 멤버만 계획 목록을 조회할 수 있다`() {
        val context = meetingContext()
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.of(context.meeting))
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
        stubPlanList(loginRole = GroupRole.MEMBER, loginGroupMemberId = 10L, planCreatorId = 10L)

        val result = getPlans()

        assertTrue(result.items.single().canDelete)
    }

    @Test
    fun `그룹 생성자는 다른 사용자의 계획도 삭제할 수 있다`() {
        stubPlanList(loginRole = GroupRole.OWNER, loginGroupMemberId = 10L, planCreatorId = 11L)

        val result = getPlans()

        assertTrue(result.items.single().canDelete)
    }

    @Test
    fun `일반 그룹 멤버는 다른 사용자의 계획을 삭제할 수 없다`() {
        stubPlanList(loginRole = GroupRole.MEMBER, loginGroupMemberId = 10L, planCreatorId = 11L)

        val result = getPlans()

        assertFalse(result.items.single().canDelete)
    }

    @Test
    fun `존재하지 않는 만남에는 계획을 생성할 수 없다`() {
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.empty())

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
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.of(context.meeting))
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
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.of(context.meeting))
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
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.of(context.meeting))
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
        loginRole: GroupRole,
        loginGroupMemberId: Long,
        planCreatorId: Long,
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
        `when`(meetingRepository.findById(1L)).thenReturn(Optional.of(context.meeting))
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(loginGroupMember)
        `when`(
            planRepository.findPageWithCreator(
                meetingId = 1L,
                category = null,
                cursor = null,
                pageable = PageRequest.of(0, 21),
            ),
        ).thenReturn(listOf(plan))
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
}
