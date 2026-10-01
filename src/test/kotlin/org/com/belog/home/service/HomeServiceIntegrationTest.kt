package org.com.belog.home.service

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.home.domain.HomeMeetingProgressStatus
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.domain.PostLog
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(HomeServiceIntegrationTest.FixedClockConfig::class)
class HomeServiceIntegrationTest {
    @Autowired
    private lateinit var homeService: HomeService

    @Autowired
    private lateinit var postLogRepository: PostLogRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var objectReadUrlProvider: S3ObjectReadUrlProvider

    @AfterEach
    fun cleanUp() {
        postLogRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `월간 달력은 조회 월과 겹치는 참여 만남만 반환한다`() {
        val group = saveGroup()
        val viewer = saveGroupMember(group, "viewer")
        val other = saveGroupMember(group, "other")
        val previousMonthMeeting = saveFixedMeeting(group, viewer, "이전 달", date(7, 31), date(8, 1))
        val insideMonthMeeting = saveFixedMeeting(group, viewer, "조회 월", date(8, 10), date(8, 10))
        val nextMonthMeeting = saveFixedMeeting(group, viewer, "다음 달", date(8, 31), date(9, 2))
        saveFixedMeeting(group, viewer, "겹치지 않음", date(7, 30), date(7, 31))
        saveSchedulingMeeting(group, viewer, "일정 조율 중")
        saveFixedMeeting(group, other, "미참여 만남", date(8, 15), date(8, 15))
        val deletedMeeting = saveFixedMeeting(group, viewer, "삭제된 만남", date(8, 16), date(8, 16))
        deletedMeeting.delete(FIXED_INSTANT)
        meetingRepository.saveAndFlush(deletedMeeting)

        val result = homeService.getCalendar(checkNotNull(viewer.user.id), YearMonth.of(2026, 8))

        assertEquals(
            listOf(previousMonthMeeting.id, insideMonthMeeting.id, nextMonthMeeting.id),
            result.meetings.map { meeting -> meeting.meetingId },
        )
    }

    @Test
    fun `진행 목록은 조율 중과 종료되지 않은 확정 만남의 상태를 날짜 경계에 맞게 반환한다`() {
        val group = saveGroup()
        val viewer = saveGroupMember(group, "viewer")
        val other = saveGroupMember(group, "other")
        val scheduling = saveSchedulingMeeting(group, viewer, "일정 조율 중")
        val endsToday = saveFixedMeeting(group, viewer, "오늘 종료", date(8, 17), date(8, 18))
        val startsToday = saveFixedMeeting(group, viewer, "오늘 시작", date(8, 18), date(8, 20))
        val upcoming = saveFixedMeeting(group, viewer, "시작 전", date(8, 19), date(8, 20))
        saveFixedMeeting(group, viewer, "종료됨", date(8, 16), date(8, 17))
        saveFixedMeeting(group, other, "미참여 만남", date(8, 19), date(8, 20))
        val deletedMeeting = saveFixedMeeting(group, viewer, "삭제된 만남", date(8, 20), date(8, 21))
        deletedMeeting.delete(FIXED_INSTANT)
        meetingRepository.saveAndFlush(deletedMeeting)

        val result = homeService.getActiveMeetings(checkNotNull(viewer.user.id), null, null, 50)

        assertEquals(
            listOf(scheduling.id, endsToday.id, startsToday.id, upcoming.id),
            result.items.map { meeting -> meeting.meetingId },
        )
        assertEquals(
            listOf(
                HomeMeetingProgressStatus.SCHEDULING,
                HomeMeetingProgressStatus.IN_PROGRESS,
                HomeMeetingProgressStatus.IN_PROGRESS,
                HomeMeetingProgressStatus.UPCOMING,
            ),
            result.items.map { meeting -> meeting.progressStatus },
        )
        assertNull(result.items.first().startDate)
        assertNull(result.items.first().endDate)
    }

    @Test
    fun `진행 목록은 조율 중에서 확정 만남으로 넘어가는 페이지에서도 중복과 누락이 없다`() {
        val group = saveGroup()
        val viewer = saveGroupMember(group, "viewer")
        val meetings =
            listOf(
                saveSchedulingMeeting(group, viewer, "조율 1"),
                saveSchedulingMeeting(group, viewer, "조율 2"),
                saveSchedulingMeeting(group, viewer, "조율 3"),
                saveFixedMeeting(group, viewer, "확정 1", date(8, 19), date(8, 20)),
                saveFixedMeeting(group, viewer, "확정 2", date(8, 19), date(8, 21)),
            )
        val viewerUserId = checkNotNull(viewer.user.id)

        val firstPage = homeService.getActiveMeetings(viewerUserId, null, null, 2)
        val secondPage = homeService.getActiveMeetings(viewerUserId, null, firstPage.nextCursor, 2)
        val thirdPage = homeService.getActiveMeetings(viewerUserId, null, secondPage.nextCursor, 2)

        assertEquals(
            meetings.map { meeting -> meeting.id },
            (firstPage.items + secondPage.items + thirdPage.items).map { meeting -> meeting.meetingId },
        )
        assertEquals(true, firstPage.hasNext)
        assertEquals(true, secondPage.hasNext)
        assertEquals(false, thirdPage.hasNext)
        assertNull(thirdPage.nextCursor)
    }

    @Test
    fun `종료 목록은 로그인 사용자가 만든 종료된 티켓만 반환한다`() {
        val group = saveGroup()
        val viewer = saveGroupMember(group, "viewer")
        val other = saveGroupMember(group, "other")
        val includedMeeting = saveFixedMeeting(group, viewer, "포함", date(8, 16), date(8, 17))
        val includedTicket = savePostLog(includedMeeting, viewer, ticketCreated = true)
        val endsToday = saveFixedMeeting(group, viewer, "오늘 종료", date(8, 17), date(8, 18))
        savePostLog(endsToday, viewer, ticketCreated = true)
        saveFixedMeeting(group, viewer, "티켓 없음", date(8, 15), date(8, 16))
        val draftMeeting = saveFixedMeeting(group, viewer, "임시 저장", date(8, 14), date(8, 15))
        savePostLog(draftMeeting, viewer, ticketCreated = false)
        val otherMeeting = saveFixedMeeting(group, other, "다른 사용자", date(8, 13), date(8, 14))
        savePostLog(otherMeeting, other, ticketCreated = true)
        val deletedMeeting = saveFixedMeeting(group, viewer, "삭제됨", date(8, 12), date(8, 13))
        savePostLog(deletedMeeting, viewer, ticketCreated = true)
        deletedMeeting.delete(FIXED_INSTANT)
        meetingRepository.saveAndFlush(deletedMeeting)

        val result = homeService.getCompletedMeetings(checkNotNull(viewer.user.id), null, 50)

        assertEquals(listOf(includedTicket.id), result.items.map { item -> item.postLogId })
    }

    @Test
    fun `종료 목록은 종료일과 Post-log ID가 같은 페이지 경계에서도 중복과 누락이 없다`() {
        val group = saveGroup()
        val viewer = saveGroupMember(group, "viewer")
        val tickets =
            listOf(
                saveTicketForDate(group, viewer, "같은 날 1", date(8, 15)),
                saveTicketForDate(group, viewer, "같은 날 2", date(8, 15)),
                saveTicketForDate(group, viewer, "같은 날 3", date(8, 15)),
                saveTicketForDate(group, viewer, "이전 날 1", date(8, 14)),
                saveTicketForDate(group, viewer, "이전 날 2", date(8, 14)),
            )
        val expectedTicketIds =
            tickets
                .sortedWith(
                    compareByDescending<PostLog> { postLog -> requireNotNull(postLog.meeting.endDate) }
                        .thenByDescending { postLog -> postLog.id },
                ).map { postLog -> postLog.id }
        val viewerUserId = checkNotNull(viewer.user.id)

        val firstPage = homeService.getCompletedMeetings(viewerUserId, null, 2)
        val secondPage = homeService.getCompletedMeetings(viewerUserId, firstPage.nextCursor, 2)
        val thirdPage = homeService.getCompletedMeetings(viewerUserId, secondPage.nextCursor, 2)

        assertEquals(
            expectedTicketIds,
            (firstPage.items + secondPage.items + thirdPage.items).map { item -> item.postLogId },
        )
        assertEquals(true, firstPage.hasNext)
        assertEquals(true, secondPage.hasNext)
        assertEquals(false, thirdPage.hasNext)
        assertNull(thirdPage.nextCursor)
    }

    private fun saveTicketForDate(
        group: Group,
        creator: GroupMember,
        name: String,
        endDate: LocalDate,
    ): PostLog {
        val meeting = saveFixedMeeting(group, creator, name, endDate.minusDays(1), endDate)
        return savePostLog(meeting, creator, ticketCreated = true)
    }

    private fun savePostLog(
        meeting: Meeting,
        creator: GroupMember,
        ticketCreated: Boolean,
    ): PostLog {
        val postLog = PostLog.create(meeting, creator)
        postLog.updateMemory("함께한 추억")
        if (ticketCreated) {
            postLog.createTicket(FIXED_INSTANT)
        }
        return postLogRepository.saveAndFlush(postLog)
    }

    private fun saveSchedulingMeeting(
        group: Group,
        creator: GroupMember,
        name: String,
    ): Meeting {
        val meeting = meetingRepository.saveAndFlush(Meeting.createPoll(group, creator, name, null))
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))
        return meeting
    }

    private fun saveFixedMeeting(
        group: Group,
        creator: GroupMember,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Meeting {
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = creator,
                    name = name,
                    location = null,
                    dateRange = MeetingDateRange(startDate, endDate),
                    confirmedAt = FIXED_INSTANT,
                    currentDate = LocalDate.of(2026, 7, 1),
                ),
            )
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))
        return meeting
    }

    private fun saveGroup(): Group =
        groupRepository.saveAndFlush(
            Group.create(
                name = "여행 모임",
                coverImageObjectKey = null,
                inviteCode = InviteCode.create("AB12CD"),
            ),
        )

    private fun saveGroupMember(
        group: Group,
        userKey: String,
    ): GroupMember = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, saveCompletedUser(userKey)))

    private fun saveCompletedUser(userKey: String): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$userKey@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "$userKey-subject",
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = userKey,
            name = userKey,
            bankAccount = BankAccount.create(Bank.KB_KOOKMIN, "123456789012", userKey),
            completedAt = Instant.parse("2026-07-01T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    private fun date(
        month: Int,
        day: Int,
    ): LocalDate = LocalDate.of(2026, month, day)

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    companion object {
        private val FIXED_INSTANT: Instant = Instant.parse("2026-08-17T15:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
