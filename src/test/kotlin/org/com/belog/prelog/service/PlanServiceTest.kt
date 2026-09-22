package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val meetingParticipantRepository = mock(MeetingParticipantRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC)
    private val planService = PlanService(meetingRepository, meetingParticipantRepository, planRepository, clock)

    @Test
    fun `존재하지 않는 만남에는 계획을 생성할 수 없다`() {
        `when`(meetingParticipantRepository.findByMeetingIdAndGroupMemberUserId(1L, 15L)).thenReturn(null)
        `when`(meetingRepository.existsById(1L)).thenReturn(false)

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
    fun `만남 참여자가 아닌 사용자는 계획을 생성할 수 없다`() {
        `when`(meetingParticipantRepository.findByMeetingIdAndGroupMemberUserId(1L, 15L)).thenReturn(null)
        `when`(meetingRepository.existsById(1L)).thenReturn(true)

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

        assertEquals(MeetingErrorCode.NOT_MEETING_PARTICIPANT, exception.errorCode)
        verifyNoInteractions(planRepository)
    }

    @Test
    fun `종료된 만남에는 계획을 생성할 수 없다`() {
        val participant = createEndedMeetingParticipant()
        `when`(meetingParticipantRepository.findByMeetingIdAndGroupMemberUserId(1L, 15L)).thenReturn(participant)

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

    private fun createEndedMeetingParticipant(): MeetingParticipant {
        val group =
            Group.create(
                name = "여행 모임",
                coverImageObjectKey = null,
                inviteCode = InviteCode.create("AB12CD"),
            )
        val member = GroupMember.createOwner(group, completedUser())
        val meeting =
            Meeting.createFixed(
                group = group,
                creator = member,
                name = "지난 만남",
                location = null,
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21)),
                confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                currentDate = LocalDate.of(2026, 9, 20),
            )
        return MeetingParticipant.create(meeting, member)
    }

    private fun completedUser(): User =
        User
            .createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = "user-subject",
            ).apply {
                completeOnboarding(
                    profileImageObjectKey = null,
                    nickname = "참여자",
                    name = "홍길동",
                    bankAccount =
                        BankAccount.create(
                            bank = Bank.KB_KOOKMIN,
                            accountNumber = "123456789012",
                            accountHolderName = "홍길동",
                        ),
                    completedAt = Instant.parse("2026-09-20T00:00:00Z"),
                )
            }
}
