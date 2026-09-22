package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.repository.PlanRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PlanServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC)
    private val planService = PlanService(meetingRepository, groupMemberRepository, planRepository, clock)

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
        `when`(group.id).thenReturn(3L)
        `when`(meeting.group).thenReturn(group)
        `when`(meeting.endDate).thenReturn(endDate)
        return MeetingContext(group, meeting)
    }

    private data class MeetingContext(
        val group: Group,
        val meeting: Meeting,
    )
}
