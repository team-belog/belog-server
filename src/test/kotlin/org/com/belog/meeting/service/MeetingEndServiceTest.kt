package org.com.belog.meeting.service

import org.com.belog.billlog.repository.BillRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MeetingEndServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val billRepository = mock(BillRepository::class.java)
    private val postLogPhotoRepository = mock(PostLogPhotoRepository::class.java)
    private val meetingEndService =
        MeetingEndService(
            meetingRepository = meetingRepository,
            planRepository = planRepository,
            billRepository = billRepository,
            postLogPhotoRepository = postLogPhotoRepository,
            clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
        )

    @Test
    fun `방장이 시작된 만남의 로그를 모두 작성했다면 만남을 종료한다`() {
        val meeting = stubMeeting()
        stubLogs(hasPreLog = true, hasBillLog = true, hasPostLog = true)

        meetingEndService.endMeeting(meetingId = MEETING_ID, userId = OWNER_USER_ID)

        assertEquals(FIXED_INSTANT, meeting.endedAt)
    }

    @Test
    fun `방장이 아니면 만남을 종료할 수 없다`() {
        val meeting = stubMeeting()
        stubLogs(hasPreLog = true, hasBillLog = true, hasPostLog = true)

        val exception =
            assertFailsWith<BusinessException> {
                meetingEndService.endMeeting(meetingId = MEETING_ID, userId = OTHER_USER_ID)
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_OWNER, exception.errorCode)
        assertNull(meeting.endedAt)
    }

    @Test
    fun `이미 종료된 만남은 다시 종료할 수 없다`() {
        val meeting = stubMeeting()
        val endedAt = Instant.parse("2026-09-21T03:00:00Z")
        meeting.end(endedAt = endedAt, currentDate = LocalDate.of(2026, 9, 21))
        stubLogs(hasPreLog = true, hasBillLog = true, hasPostLog = true)

        val exception =
            assertFailsWith<BusinessException> {
                meetingEndService.endMeeting(meetingId = MEETING_ID, userId = OWNER_USER_ID)
            }

        assertEquals(MeetingErrorCode.MEETING_END_ALREADY_ENDED, exception.errorCode)
        assertEquals(endedAt, meeting.endedAt)
    }

    @Test
    fun `만남 시작일 이전에는 만남을 종료할 수 없다`() {
        val meeting = stubMeeting(startDate = LocalDate.of(2026, 9, 23))
        stubLogs(hasPreLog = true, hasBillLog = true, hasPostLog = true)

        val exception =
            assertFailsWith<BusinessException> {
                meetingEndService.endMeeting(meetingId = MEETING_ID, userId = OWNER_USER_ID)
            }

        assertEquals(MeetingErrorCode.MEETING_END_NOT_STARTED, exception.errorCode)
        assertNull(meeting.endedAt)
    }

    @ParameterizedTest
    @CsvSource(
        "false, true, true",
        "true, false, true",
        "true, true, false",
    )
    fun `작성하지 않은 로그가 있으면 만남을 종료할 수 없다`(
        hasPreLog: Boolean,
        hasBillLog: Boolean,
        hasPostLog: Boolean,
    ) {
        val meeting = stubMeeting()
        stubLogs(hasPreLog = hasPreLog, hasBillLog = hasBillLog, hasPostLog = hasPostLog)

        val exception =
            assertFailsWith<BusinessException> {
                meetingEndService.endMeeting(meetingId = MEETING_ID, userId = OWNER_USER_ID)
            }

        assertEquals(MeetingErrorCode.MEETING_END_LOGS_INCOMPLETE, exception.errorCode)
        assertNull(meeting.endedAt)
    }

    private fun stubMeeting(startDate: LocalDate = LocalDate.of(2026, 9, 21)): Meeting {
        val group =
            Group.create(
                name = "주말 여행 모임",
                coverImageObjectKey = null,
                inviteCode = InviteCode.create("AB12CD"),
            )
        val ownerUser =
            User
                .createSocialUser(
                    email = "owner@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "owner-subject",
                ).apply {
                    completeOnboarding(
                        profileImageObjectKey = null,
                        nickname = "방장",
                        lastName = "홍",
                        firstName = "길동",
                        bankAccount =
                            BankAccount.create(
                                bank = Bank.KB_KOOKMIN,
                                accountNumber = "123456789012",
                            ),
                        completedAt = Instant.parse("2026-09-15T00:00:00Z"),
                    )
                }
        ReflectionTestUtils.setField(ownerUser, "id", OWNER_USER_ID)
        val meeting =
            Meeting.createFixed(
                group = group,
                creator = GroupMember.createOwner(group, ownerUser),
                name = "광주 여행",
                location = null,
                dateRange = MeetingDateRange(startDate, startDate.plusDays(1)),
                confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                currentDate = LocalDate.of(2026, 9, 20),
            )
        `when`(meetingRepository.findByIdForUpdate(MEETING_ID)).thenReturn(meeting)
        return meeting
    }

    private fun stubLogs(
        hasPreLog: Boolean,
        hasBillLog: Boolean,
        hasPostLog: Boolean,
    ) {
        `when`(planRepository.existsByMeetingId(MEETING_ID)).thenReturn(hasPreLog)
        `when`(billRepository.existsByMeetingId(MEETING_ID)).thenReturn(hasBillLog)
        `when`(postLogPhotoRepository.existsByMeetingId(MEETING_ID)).thenReturn(hasPostLog)
    }

    companion object {
        private const val MEETING_ID = 7L
        private const val OWNER_USER_ID = 15L
        private const val OTHER_USER_ID = 16L
        private val FIXED_INSTANT: Instant = Instant.parse("2026-09-22T03:00:00Z")
    }
}
