package org.com.belog.meeting.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    MeetingService::class,
    MeetingServiceTest.FixedClockConfig::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class MeetingServiceTest {
    @Autowired
    private lateinit var meetingService: MeetingService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `그룹 멤버가 확정 날짜 만남을 생성하면 생성자와 선택한 멤버가 참여자로 저장된다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val participant = saveGroupMember(group, "participant-subject", "참여자")

        val result =
            meetingService.createFixedMeeting(
                groupId = requireNotNull(group.id),
                creatorUserId = requireNotNull(creator.user.id),
                name = "  광주 여행  ",
                location = "  서울고속버스터미널  ",
                participantMemberIds = listOf(requireNotNull(participant.id)),
                startDate = LocalDate.of(2026, 9, 22),
                endDate = LocalDate.of(2026, 9, 23),
            )

        assertEquals("광주 여행", result.name)
        assertEquals("서울고속버스터미널", result.location)
        assertEquals(MeetingScheduleType.FIXED, result.scheduleType)
        assertEquals(MeetingStatus.CONFIRMED, result.status)
        assertEquals(LocalDate.of(2026, 9, 22), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 23), result.endDate)
        assertEquals(FIXED_INSTANT, result.confirmedAt)
        assertEquals(2, result.participantCount)
        assertEquals(1L, meetingRepository.count())
        assertEquals(2L, meetingParticipantRepository.count())
    }

    @Test
    fun `그룹 방장이 아니어도 같은 그룹 멤버라면 만남을 생성할 수 있다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val result =
            meetingService.createFixedMeeting(
                groupId = requireNotNull(group.id),
                creatorUserId = requireNotNull(creator.user.id),
                name = "광주 여행",
                location = null,
                participantMemberIds = emptyList(),
                startDate = LocalDate.of(2026, 9, 21),
                endDate = LocalDate.of(2026, 9, 21),
            )

        assertEquals(1, result.participantCount)
        assertEquals(1L, meetingParticipantRepository.count())
    }

    @Test
    fun `그룹 멤버가 일정 조율 만남을 생성하면 참여자와 후보 일정 범위가 저장된다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val participant = saveGroupMember(group, "participant-subject", "참여자")

        val result =
            meetingService.createPollMeeting(
                groupId = requireNotNull(group.id),
                creatorUserId = requireNotNull(creator.user.id),
                name = "  광주 여행  ",
                location = "  서울고속버스터미널  ",
                participantMemberIds = listOf(requireNotNull(participant.id)),
                candidateDateRanges =
                    listOf(
                        MeetingDateRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23)),
                        MeetingDateRange(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30)),
                    ),
            )

        assertEquals("광주 여행", result.name)
        assertEquals("서울고속버스터미널", result.location)
        assertEquals(MeetingScheduleType.POLL, result.scheduleType)
        assertEquals(MeetingStatus.SCHEDULING, result.status)
        assertEquals(null, result.startDate)
        assertEquals(null, result.endDate)
        assertEquals(null, result.confirmedAt)
        assertEquals(2, result.participantCount)
        assertEquals(1L, meetingRepository.count())
        assertEquals(2L, meetingParticipantRepository.count())
        assertEquals(2L, meetingCandidateDateRangeRepository.count())
    }

    @Test
    fun `후보 일정 범위가 두 개 미만이면 일정 조율 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                createPollMeeting(
                    group = group,
                    creator = creator,
                    candidateDateRanges =
                        listOf(MeetingDateRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23))),
                )
            }

        assertEquals(MeetingErrorCode.INVALID_CANDIDATE_DATE_RANGE_COUNT, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `중복된 후보 일정 범위가 있으면 일정 조율 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val candidateDateRange =
            MeetingDateRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23))

        val exception =
            assertFailsWith<BusinessException> {
                createPollMeeting(
                    group = group,
                    creator = creator,
                    candidateDateRanges = listOf(candidateDateRange, candidateDateRange),
                )
            }

        assertEquals(MeetingErrorCode.DUPLICATE_CANDIDATE_DATE_RANGE, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `후보 일정에 과거 날짜가 있으면 일정 조율 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val pastDateException =
            assertFailsWith<BusinessException> {
                createPollMeeting(
                    group = group,
                    creator = creator,
                    candidateDateRanges =
                        listOf(
                            MeetingDateRange(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 20)),
                            MeetingDateRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23)),
                        ),
                )
            }
        assertEquals(MeetingErrorCode.PAST_MEETING_DATE, pastDateException.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `그룹 멤버가 아닌 사용자는 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val otherUser = saveCompletedUser("other-subject", "외부인")

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.createFixedMeeting(
                    groupId = requireNotNull(group.id),
                    creatorUserId = requireNotNull(otherUser.id),
                    name = "광주 여행",
                    location = null,
                    participantMemberIds = emptyList(),
                    startDate = LocalDate.of(2026, 9, 21),
                    endDate = LocalDate.of(2026, 9, 21),
                )
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `존재하지 않는 그룹에는 만남을 생성할 수 없다`() {
        val user = saveCompletedUser("creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.createFixedMeeting(
                    groupId = 999L,
                    creatorUserId = requireNotNull(user.id),
                    name = "광주 여행",
                    location = null,
                    participantMemberIds = emptyList(),
                    startDate = LocalDate.of(2026, 9, 21),
                    endDate = LocalDate.of(2026, 9, 21),
                )
            }

        assertEquals(GroupErrorCode.GROUP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `중복된 참여자 ID가 포함되면 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val participant = saveGroupMember(group, "participant-subject", "참여자")
        val participantId = requireNotNull(participant.id)

        val exception =
            assertFailsWith<BusinessException> {
                createMeeting(group, creator, listOf(participantId, participantId))
            }

        assertEquals(MeetingErrorCode.DUPLICATE_PARTICIPANT, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `생성자가 참여자 ID 목록에 포함되면 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                createMeeting(group, creator, listOf(requireNotNull(creator.id)))
            }

        assertEquals(MeetingErrorCode.CREATOR_INCLUDED_AS_PARTICIPANT, exception.errorCode)
    }

    @Test
    fun `생성자를 제외한 참여자가 14명을 초과하면 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                createMeeting(group, creator, (1001L..1015L).toList())
            }

        assertEquals(MeetingErrorCode.PARTICIPANT_LIMIT_EXCEEDED, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `다른 그룹의 멤버가 참여자에 포함되면 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val otherGroup = groupRepository.save(createGroup("EF34GH"))
        val otherMember = saveGroupMember(otherGroup, "other-subject", "다른멤버")

        val exception =
            assertFailsWith<BusinessException> {
                createMeeting(group, creator, listOf(requireNotNull(otherMember.id)))
            }

        assertEquals(MeetingErrorCode.INVALID_PARTICIPANT, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `과거 날짜로 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.createFixedMeeting(
                    groupId = requireNotNull(group.id),
                    creatorUserId = requireNotNull(creator.user.id),
                    name = "광주 여행",
                    location = null,
                    participantMemberIds = emptyList(),
                    startDate = LocalDate.of(2026, 9, 20),
                    endDate = LocalDate.of(2026, 9, 20),
                )
            }

        assertEquals(MeetingErrorCode.PAST_MEETING_DATE, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `종료일이 시작일보다 빠르면 만남을 생성할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.createFixedMeeting(
                    groupId = requireNotNull(group.id),
                    creatorUserId = requireNotNull(creator.user.id),
                    name = "광주 여행",
                    location = null,
                    participantMemberIds = emptyList(),
                    startDate = LocalDate.of(2026, 9, 22),
                    endDate = LocalDate.of(2026, 9, 21),
                )
            }

        assertEquals(MeetingErrorCode.INVALID_MEETING_DATE_RANGE, exception.errorCode)
        assertEquals(0L, meetingRepository.count())
    }

    @Test
    fun `만남 생성자가 후보 일정을 최종 일정으로 확정한다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meeting = meetingRepository.save(createPollMeeting(group, creator))
        val candidateDateRange =
            meetingCandidateDateRangeRepository.save(
                createCandidateDateRange(
                    meeting = meeting,
                    startDate = LocalDate.of(2026, 9, 22),
                    endDate = LocalDate.of(2026, 9, 23),
                ),
            )

        val changed =
            meetingService.confirmMeetingDate(
                meetingId = requireNotNull(meeting.id),
                userId = requireNotNull(creator.user.id),
                candidateDateRangeId = requireNotNull(candidateDateRange.id),
            )

        assertTrue(changed)
        assertEquals(MeetingStatus.CONFIRMED, meeting.status)
        assertEquals(candidateDateRange.startDate, meeting.startDate)
        assertEquals(candidateDateRange.endDate, meeting.endDate)
        assertEquals(FIXED_INSTANT, meeting.confirmedAt)
    }

    @Test
    fun `만남 생성자가 확정된 일정을 수정한다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meeting =
            meetingRepository.save(
                createFixedMeeting(
                    group = group,
                    creator = creator,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23)),
                ),
            )
        val firstConfirmedAt = meeting.confirmedAt

        val changed =
            meetingService.updateMeetingDate(
                meetingId = requireNotNull(meeting.id),
                userId = requireNotNull(creator.user.id),
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25)),
            )

        assertTrue(changed)
        assertEquals(LocalDate.of(2026, 9, 24), meeting.startDate)
        assertEquals(LocalDate.of(2026, 9, 25), meeting.endDate)
        assertEquals(firstConfirmedAt, meeting.confirmedAt)
    }

    @Test
    fun `종료된 만남의 일정은 수정할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meeting =
            meetingRepository.save(
                createFixedMeeting(
                    group = group,
                    creator = creator,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20)),
                    currentDate = LocalDate.of(2026, 9, 19),
                ),
            )

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.updateMeetingDate(
                    meetingId = requireNotNull(meeting.id),
                    userId = requireNotNull(creator.user.id),
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25)),
                )
            }

        assertEquals(MeetingErrorCode.MEETING_ALREADY_ENDED, exception.errorCode)
    }

    @Test
    fun `생성자가 아닌 사용자는 후보 일정을 확정할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val member = saveGroupMember(group, "member-subject", "멤버")
        val meeting = meetingRepository.save(createPollMeeting(group, creator))
        val candidateDateRange =
            meetingCandidateDateRangeRepository.save(
                createCandidateDateRange(
                    meeting = meeting,
                    startDate = LocalDate.of(2026, 9, 22),
                    endDate = LocalDate.of(2026, 9, 23),
                ),
            )

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.confirmMeetingDate(
                    meetingId = requireNotNull(meeting.id),
                    userId = requireNotNull(member.user.id),
                    candidateDateRangeId = requireNotNull(candidateDateRange.id),
                )
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_CREATOR, exception.errorCode)
        assertEquals(MeetingStatus.SCHEDULING, meeting.status)
    }

    private fun createMeeting(
        group: Group,
        creator: GroupMember,
        participantMemberIds: List<Long>,
    ) = meetingService.createFixedMeeting(
        groupId = requireNotNull(group.id),
        creatorUserId = requireNotNull(creator.user.id),
        name = "광주 여행",
        location = null,
        participantMemberIds = participantMemberIds,
        startDate = LocalDate.of(2026, 9, 21),
        endDate = LocalDate.of(2026, 9, 21),
    )

    private fun createPollMeeting(
        group: Group,
        creator: GroupMember,
        candidateDateRanges: List<MeetingDateRange>,
    ) = meetingService.createPollMeeting(
        groupId = requireNotNull(group.id),
        creatorUserId = requireNotNull(creator.user.id),
        name = "광주 여행",
        location = null,
        participantMemberIds = emptyList(),
        candidateDateRanges = candidateDateRanges,
    )

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun createPollMeeting(
        group: Group,
        creator: GroupMember,
    ): Meeting =
        Meeting.createPoll(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
        )

    private fun createFixedMeeting(
        group: Group,
        creator: GroupMember,
        dateRange: MeetingDateRange,
        currentDate: LocalDate = LocalDate.of(2026, 9, 21),
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            dateRange = dateRange,
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = currentDate,
        )

    private fun createCandidateDateRange(
        meeting: Meeting,
        startDate: LocalDate,
        endDate: LocalDate,
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            dateRange = MeetingDateRange(startDate, endDate),
            currentDate = LocalDate.of(2026, 9, 21),
        )

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember {
        val user = saveCompletedUser(providerUserId, nickname)
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
    ): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$providerUserId@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = providerUserId,
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = nickname,
            name = "홍길동",
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    companion object {
        private val FIXED_INSTANT: Instant = Instant.parse("2026-09-21T00:00:00Z")
    }
}
