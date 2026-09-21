package org.com.belog.meeting.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingAvailableDateRepository
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
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

@DataJpaTest
@ActiveProfiles("test")
@Import(
    MeetingDatePollService::class,
    MeetingDatePollServiceTest.FixedClockConfig::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class MeetingDatePollServiceTest {
    @Autowired
    private lateinit var meetingDatePollService: MeetingDatePollService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository

    @Autowired
    private lateinit var meetingScheduleResponseRepository: MeetingScheduleResponseRepository

    @Autowired
    private lateinit var meetingAvailableDateRepository: MeetingAvailableDateRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `참여자는 후보 일정을 조회한다`() {
        val context = savePollContext()

        val result =
            meetingDatePollService.getDatePoll(
                meetingId = requireNotNull(context.meeting.id),
                userId = requireNotNull(context.member.user.id),
            )

        assertEquals(context.candidates.map { it.id }, result.candidateDateRanges.map { it.id })
    }

    @Test
    fun `참여자가 복수 후보 일정에 응답하면 완료 정보와 선택 날짜를 저장한다`() {
        val context = savePollContext()
        val selectedCandidateIds = context.candidates.map { requireNotNull(it.id) }

        meetingDatePollService.respondDatePoll(
            meetingId = requireNotNull(context.meeting.id),
            userId = requireNotNull(context.member.user.id),
            candidateDateRangeIds = selectedCandidateIds,
        )

        assertEquals(1L, meetingScheduleResponseRepository.count())
        assertEquals(2L, meetingAvailableDateRepository.count())
    }

    @Test
    fun `빈 선택으로 응답하면 미응답과 구분되는 완료 상태를 저장한다`() {
        val context = savePollContext()

        meetingDatePollService.respondDatePoll(
            meetingId = requireNotNull(context.meeting.id),
            userId = requireNotNull(context.member.user.id),
            candidateDateRangeIds = emptyList(),
        )

        assertEquals(1L, meetingScheduleResponseRepository.count())
        assertEquals(0L, meetingAvailableDateRepository.count())
    }

    @Test
    fun `만남 참여자가 아닌 사용자는 후보 일정을 조회할 수 없다`() {
        val context = savePollContext()
        val otherUser = saveCompletedUser("other-subject", "외부인")

        val exception =
            assertFailsWith<BusinessException> {
                meetingDatePollService.getDatePoll(
                    meetingId = requireNotNull(context.meeting.id),
                    userId = requireNotNull(otherUser.id),
                )
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_PARTICIPANT, exception.errorCode)
    }

    @Test
    fun `만남 생성자는 후보 일정에 응답할 수 없다`() {
        val context = savePollContext()

        val exception =
            assertFailsWith<BusinessException> {
                meetingDatePollService.respondDatePoll(
                    meetingId = requireNotNull(context.meeting.id),
                    userId = requireNotNull(context.creator.user.id),
                    candidateDateRangeIds = emptyList(),
                )
            }

        assertEquals(MeetingErrorCode.CREATOR_CANNOT_RESPOND_DATE_POLL, exception.errorCode)
        assertEquals(0L, meetingScheduleResponseRepository.count())
    }

    @Test
    fun `다른 만남의 후보 일정 ID로 응답할 수 없다`() {
        val context = savePollContext()
        val otherMeeting = meetingRepository.saveAndFlush(createPollMeeting(context.group, context.creator))
        val otherCandidate =
            meetingCandidateDateRangeRepository.saveAndFlush(
                createCandidateDateRange(
                    meeting = otherMeeting,
                    startDate = LocalDate.of(2026, 10, 6),
                    endDate = LocalDate.of(2026, 10, 7),
                ),
            )

        val exception =
            assertFailsWith<BusinessException> {
                meetingDatePollService.respondDatePoll(
                    meetingId = requireNotNull(context.meeting.id),
                    userId = requireNotNull(context.member.user.id),
                    candidateDateRangeIds = listOf(requireNotNull(otherCandidate.id)),
                )
            }

        assertEquals(MeetingErrorCode.INVALID_AVAILABLE_DATE, exception.errorCode)
        assertEquals(0L, meetingScheduleResponseRepository.count())
    }

    @Test
    fun `중복된 후보 일정 ID로 응답할 수 없다`() {
        val context = savePollContext()
        val candidateId = requireNotNull(context.candidates.first().id)

        val exception =
            assertFailsWith<BusinessException> {
                meetingDatePollService.respondDatePoll(
                    meetingId = requireNotNull(context.meeting.id),
                    userId = requireNotNull(context.member.user.id),
                    candidateDateRangeIds = listOf(candidateId, candidateId),
                )
            }

        assertEquals(MeetingErrorCode.DUPLICATE_AVAILABLE_DATE, exception.errorCode)
        assertEquals(0L, meetingScheduleResponseRepository.count())
    }

    @Test
    fun `응답을 완료한 참여자는 다시 응답할 수 없다`() {
        val context = savePollContext()
        meetingDatePollService.respondDatePoll(
            meetingId = requireNotNull(context.meeting.id),
            userId = requireNotNull(context.member.user.id),
            candidateDateRangeIds = emptyList(),
        )

        val exception =
            assertFailsWith<BusinessException> {
                meetingDatePollService.respondDatePoll(
                    meetingId = requireNotNull(context.meeting.id),
                    userId = requireNotNull(context.member.user.id),
                    candidateDateRangeIds = listOf(requireNotNull(context.candidates.first().id)),
                )
            }

        assertEquals(MeetingErrorCode.DATE_POLL_ALREADY_RESPONDED, exception.errorCode)
        assertEquals(1L, meetingScheduleResponseRepository.count())
        assertEquals(0L, meetingAvailableDateRepository.count())
    }

    private fun savePollContext(): PollContext {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        val creatorParticipant = MeetingParticipant.create(meeting, creator)
        val memberParticipant = MeetingParticipant.create(meeting, member)
        meetingParticipantRepository.saveAllAndFlush(listOf(creatorParticipant, memberParticipant))
        val candidates =
            meetingCandidateDateRangeRepository.saveAllAndFlush(
                listOf(
                    createCandidateDateRange(
                        meeting,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                    ),
                    createCandidateDateRange(
                        meeting,
                        LocalDate.of(2026, 10, 3),
                        LocalDate.of(2026, 10, 4),
                    ),
                ),
            )
        return PollContext(group, creator, member, meeting, candidates)
    }

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

    private fun createCandidateDateRange(
        meeting: Meeting,
        startDate: LocalDate,
        endDate: LocalDate,
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            startDate = startDate,
            endDate = endDate,
            currentDate = LocalDate.of(2026, 9, 22),
        )

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
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
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-15T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    data class PollContext(
        val group: Group,
        val creator: GroupMember,
        val member: GroupMember,
        val meeting: Meeting,
        val candidates: List<MeetingCandidateDateRange>,
    )

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    companion object {
        private val FIXED_INSTANT: Instant = Instant.parse("2026-09-22T00:00:00Z")
    }
}
