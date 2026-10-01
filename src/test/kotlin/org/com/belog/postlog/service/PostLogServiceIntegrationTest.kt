package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoLike
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.repository.PostLogPhotoLikeRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(PostLogServiceIntegrationTest.FixedClockConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostLogServiceIntegrationTest {
    @Autowired
    private lateinit var postLogService: PostLogService

    @Autowired
    private lateinit var postLogRepository: PostLogRepository

    @Autowired
    private lateinit var photoRepository: PostLogPhotoRepository

    @Autowired
    private lateinit var photoLikeRepository: PostLogPhotoLikeRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

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
        photoLikeRepository.deleteAllInBatch()
        photoRepository.deleteAllInBatch()
        postLogRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
        reset(objectReadUrlProvider)
    }

    @Test
    fun `그룹 멤버가 최초 임시저장하면 미발행 Post-log가 생성된다`() {
        val context = saveMeetingContext()

        postLogService.saveDraft(
            meetingId = context.meetingId,
            userId = context.creatorUserId,
            memory = "  함께한 광주 여행  ",
        )

        val savedPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        assertNotNull(savedPostLog)
        assertEquals("함께한 광주 여행", savedPostLog.memory)
        assertNull(savedPostLog.ticketCreatedAt)
        assertFalse(savedPostLog.isTicketCreated)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `같은 사용자가 다시 임시저장하면 기존 Post-log의 문구만 변경된다`() {
        val context = saveMeetingContext()
        postLogService.saveDraft(context.meetingId, context.creatorUserId, "첫 번째 초안")
        val initialPostLog =
            checkNotNull(
                postLogRepository.findByMeetingIdAndCreatedById(
                    context.meetingId,
                    checkNotNull(context.creator.id),
                ),
            )

        postLogService.saveDraft(context.meetingId, context.creatorUserId, "두 번째 초안")

        val updatedPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        assertNotNull(updatedPostLog)
        assertEquals(initialPostLog.id, updatedPostLog.id)
        assertEquals("두 번째 초안", updatedPostLog.memory)
        assertNull(updatedPostLog.ticketCreatedAt)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `서로 다른 그룹 멤버는 같은 만남에 각자의 초안을 저장한다`() {
        val context = saveMeetingContext()
        val otherMember = saveGroupMember(context.group, "member2")

        postLogService.saveDraft(context.meetingId, context.creatorUserId, "생성자의 초안")
        postLogService.saveDraft(
            meetingId = context.meetingId,
            userId = checkNotNull(otherMember.user.id),
            memory = "다른 멤버의 초안",
        )

        val creatorPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        val otherMemberPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(otherMember.id),
            )
        assertEquals(2L, postLogRepository.count())
        assertEquals("생성자의 초안", creatorPostLog?.memory)
        assertEquals("다른 멤버의 초안", otherMemberPostLog?.memory)
        assertTrue(creatorPostLog?.id != otherMemberPostLog?.id)
    }

    @Test
    fun `티켓을 생성한 사용자는 추억 문구를 임시저장할 수 없다`() {
        val context = saveMeetingContext()
        postLogService.createTicket(context.meetingId, context.creatorUserId, "최종 티켓 문구")

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.saveDraft(context.meetingId, context.creatorUserId, "변경하려는 초안")
            }

        val savedPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        assertEquals(PostLogErrorCode.TICKET_ALREADY_CREATED, exception.errorCode)
        assertEquals("최종 티켓 문구", savedPostLog?.memory)
        assertEquals(FIXED_INSTANT, savedPostLog?.ticketCreatedAt)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `임시저장된 Post-log로 티켓을 생성하면 같은 행이 발행 상태로 변경된다`() {
        val context = saveMeetingContext()
        postLogService.saveDraft(context.meetingId, context.creatorUserId, "임시저장 문구")
        val draftPostLog =
            checkNotNull(
                postLogRepository.findByMeetingIdAndCreatedById(
                    context.meetingId,
                    checkNotNull(context.creator.id),
                ),
            )

        val result = postLogService.createTicket(context.meetingId, context.creatorUserId, "최종 티켓 문구")

        val createdPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        assertNotNull(createdPostLog)
        assertEquals(draftPostLog.id, createdPostLog.id)
        assertEquals(result.postLogId, createdPostLog.id)
        assertEquals("최종 티켓 문구", createdPostLog.memory)
        assertEquals(FIXED_INSTANT, createdPostLog.ticketCreatedAt)
        assertTrue(createdPostLog.isTicketCreated)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `만남 참여자가 티켓을 최초 생성하면 추억 문구와 생성 상태가 저장된다`() {
        val context = saveMeetingContext()

        val result =
            postLogService.createTicket(
                meetingId = context.meetingId,
                userId = context.creatorUserId,
                memory = "  함께한 광주 여행  ",
            )

        val savedPostLog = postLogRepository.findByMeetingIdAndCreatedById(context.meetingId, checkNotNull(context.creator.id))
        assertNotNull(savedPostLog)
        assertEquals("함께한 광주 여행", savedPostLog.memory)
        assertEquals(FIXED_INSTANT, savedPostLog.ticketCreatedAt)
        assertTrue(savedPostLog.isTicketCreated)
        assertEquals(context.meetingId, result.meetingId)
        assertEquals("광주 여행", result.meetingName)
        assertEquals("함께한 광주 여행", result.memory)
        assertEquals(LocalDate.of(2026, 9, 28), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 29), result.endDate)
        assertEquals("광주", result.location)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `이미 티켓이 생성된 만남에는 다시 생성할 수 없다`() {
        val context = saveMeetingContext()
        postLogService.createTicket(context.meetingId, context.creatorUserId, "첫 번째 추억")

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.createTicket(context.meetingId, context.creatorUserId, "변경하려는 추억")
            }

        val savedPostLog = postLogRepository.findByMeetingIdAndCreatedById(context.meetingId, checkNotNull(context.creator.id))
        assertEquals(PostLogErrorCode.TICKET_ALREADY_CREATED, exception.errorCode)
        assertEquals("첫 번째 추억", savedPostLog?.memory)
        assertEquals(FIXED_INSTANT, savedPostLog?.ticketCreatedAt)
        assertEquals(1L, postLogRepository.count())
    }

    @Test
    fun `같은 그룹 멤버여도 만남 참여자가 아니면 티켓을 생성할 수 없다`() {
        val context = saveMeetingContext()
        val nonParticipant = saveGroupMember(context.group, "writer")

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.createTicket(
                    meetingId = context.meetingId,
                    userId = checkNotNull(nonParticipant.user.id),
                    memory = "비참여 그룹 멤버의 추억",
                )
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_PARTICIPANT, exception.errorCode)
        assertEquals(0L, postLogRepository.count())
    }

    @Test
    fun `서로 다른 만남 참여자는 같은 만남에 각자의 티켓을 생성할 수 있다`() {
        val context = saveMeetingContext()
        val otherMember = saveGroupMember(context.group, "member2")
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(context.meeting, otherMember))

        val creatorResult =
            postLogService.createTicket(
                meetingId = context.meetingId,
                userId = context.creatorUserId,
                memory = "생성자의 추억",
            )
        val otherMemberResult =
            postLogService.createTicket(
                meetingId = context.meetingId,
                userId = checkNotNull(otherMember.user.id),
                memory = "다른 멤버의 추억",
            )

        val creatorPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(context.creator.id),
            )
        val otherMemberPostLog =
            postLogRepository.findByMeetingIdAndCreatedById(
                context.meetingId,
                checkNotNull(otherMember.id),
            )

        assertEquals(2L, postLogRepository.count())
        assertTrue(creatorResult.postLogId != otherMemberResult.postLogId)
        assertEquals("생성자의 추억", creatorPostLog?.memory)
        assertEquals("다른 멤버의 추억", otherMemberPostLog?.memory)
        assertEquals(creatorResult.postLogId, creatorPostLog?.id)
        assertEquals(otherMemberResult.postLogId, otherMemberPostLog?.id)
    }

    @Test
    fun `만남 참여자가 아닌 외부 사용자는 티켓을 생성할 수 없다`() {
        val context = saveMeetingContext()
        val outsider = saveCompletedUser("outsider")

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.createTicket(
                    meetingId = context.meetingId,
                    userId = checkNotNull(outsider.id),
                    memory = "외부 사용자의 추억",
                )
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_PARTICIPANT, exception.errorCode)
        assertEquals(0L, postLogRepository.count())
    }

    @Test
    fun `티켓을 생성한 그룹 멤버는 본인 티켓을 조회한다`() {
        val context = saveMeetingContext()
        val createdTicket =
            postLogService.createTicket(
                meetingId = context.meetingId,
                userId = context.creatorUserId,
                memory = "함께한 광주 여행",
            )

        val result = postLogService.getTicket(createdTicket.postLogId, context.creatorUserId)

        assertEquals(createdTicket.postLogId, result.postLogId)
        assertEquals(context.meetingId, result.meetingId)
        assertEquals("광주 여행", result.meetingName)
        assertEquals("함께한 광주 여행", result.memory)
        assertEquals(LocalDate.of(2026, 9, 28), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 29), result.endDate)
        assertEquals("광주", result.location)
    }

    @Test
    fun `임시저장만 한 Post-log는 티켓으로 조회할 수 없다`() {
        val context = saveMeetingContext()
        postLogService.saveDraft(context.meetingId, context.creatorUserId, "임시저장 문구")
        val draftPostLog =
            checkNotNull(
                postLogRepository.findByMeetingIdAndCreatedById(
                    context.meetingId,
                    checkNotNull(context.creator.id),
                ),
            )

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.getTicket(checkNotNull(draftPostLog.id), context.creatorUserId)
            }

        assertEquals(PostLogErrorCode.TICKET_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `서로 다른 그룹 멤버는 같은 만남에서 본인 티켓만 조회한다`() {
        val context = saveMeetingContext()
        val otherMember = saveGroupMember(context.group, "member2")
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(context.meeting, otherMember))
        val otherUserId = checkNotNull(otherMember.user.id)
        val creatorTicket =
            postLogService.createTicket(context.meetingId, context.creatorUserId, "생성자의 추억")
        val otherMemberTicket =
            postLogService.createTicket(context.meetingId, otherUserId, "다른 멤버의 추억")

        val creatorResult = postLogService.getTicket(creatorTicket.postLogId, context.creatorUserId)
        val otherMemberResult = postLogService.getTicket(otherMemberTicket.postLogId, otherUserId)
        val exception =
            assertFailsWith<BusinessException> {
                postLogService.getTicket(otherMemberTicket.postLogId, context.creatorUserId)
            }

        assertEquals(creatorTicket.postLogId, creatorResult.postLogId)
        assertEquals("생성자의 추억", creatorResult.memory)
        assertEquals(otherMemberTicket.postLogId, otherMemberResult.postLogId)
        assertEquals("다른 멤버의 추억", otherMemberResult.memory)
        assertEquals(PostLogErrorCode.TICKET_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `그룹 멤버가 아닌 사용자는 티켓을 조회할 수 없다`() {
        val context = saveMeetingContext()
        val outsider = saveCompletedUser("outsider")
        val createdTicket =
            postLogService.createTicket(context.meetingId, context.creatorUserId, "생성자의 추억")

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.getTicket(createdTicket.postLogId, checkNotNull(outsider.id))
            }

        assertEquals(PostLogErrorCode.TICKET_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 Post-log의 티켓은 조회할 수 없다`() {
        val context = saveMeetingContext()

        val exception =
            assertFailsWith<BusinessException> {
                postLogService.getTicket(Long.MAX_VALUE, context.creatorUserId)
            }

        assertEquals(PostLogErrorCode.TICKET_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `좋아요 수가 가장 많고 동률이면 먼저 등록된 사진을 대표 사진으로 선택한다`() {
        val context = saveMeetingContext()
        val firstPhoto = savePhoto(context, "first.jpg", "2026-09-28T14:00:00+09:00")
        val secondPhoto = savePhoto(context, "second.jpg", "2026-09-28T15:00:00+09:00")
        val thirdPhoto = savePhoto(context, "third.jpg", "2026-09-28T16:00:00+09:00")
        val firstLiker = saveGroupMember(context.group, "liker1")
        val secondLiker = saveGroupMember(context.group, "liker2")
        photoLikeRepository.saveAllAndFlush(
            listOf(
                PostLogPhotoLike.create(firstPhoto, context.creator),
                PostLogPhotoLike.create(firstPhoto, firstLiker),
                PostLogPhotoLike.create(secondPhoto, context.creator),
                PostLogPhotoLike.create(secondPhoto, secondLiker),
                PostLogPhotoLike.create(thirdPhoto, context.creator),
            ),
        )
        `when`(objectReadUrlProvider.generateReadUrl(firstPhoto.objectKey)).thenReturn(REPRESENTATIVE_PHOTO_URL)

        val result = postLogService.createTicket(context.meetingId, context.creatorUserId, "대표 사진이 있는 추억")

        assertEquals(REPRESENTATIVE_PHOTO_URL, result.coverPhotoUrl)
        verify(objectReadUrlProvider).generateReadUrl(firstPhoto.objectKey)
    }

    @Test
    fun `티켓에는 그룹 전체가 아닌 만남 참여 멤버만 반환한다`() {
        val context = saveMeetingContext()
        val participant = saveGroupMember(context.group, "member2")
        saveGroupMember(context.group, "member3")
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(context.meeting, participant))

        val result =
            postLogService.createTicket(
                meetingId = context.meetingId,
                userId = context.creatorUserId,
                memory = "참여 멤버와 함께한 추억",
            )

        assertEquals(
            listOf(checkNotNull(context.creator.id), checkNotNull(participant.id)),
            result.members.map { member -> member.groupMemberId },
        )
        assertEquals(listOf("creator", "member2"), result.members.map { member -> member.nickname })
    }

    private fun saveMeetingContext(): MeetingContext {
        val group =
            groupRepository.save(
                Group.create(
                    name = "주말 여행 모임",
                    coverImageObjectKey = null,
                    inviteCode = InviteCode.create("AB12CD"),
                ),
            )
        val creator = saveGroupMember(group, "creator")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = creator,
                    name = "광주 여행",
                    location = "광주",
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))

        return MeetingContext(
            group = group,
            creator = creator,
            meeting = meeting,
            meetingId = checkNotNull(meeting.id),
            creatorUserId = checkNotNull(creator.user.id),
        )
    }

    private fun savePhoto(
        context: MeetingContext,
        fileName: String,
        capturedAt: String,
    ): PostLogPhoto =
        photoRepository.saveAndFlush(
            PostLogPhoto.create(
                meeting = context.meeting,
                uploader = context.creator,
                objectKey =
                    PostLogPhotoObjectKey.create(
                        context.meetingId,
                        "post-logs/${context.meetingId}/photos/$fileName",
                    ),
                capturedAt = OffsetDateTime.parse(capturedAt),
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
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = userKey,
                ),
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    private data class MeetingContext(
        val group: Group,
        val creator: GroupMember,
        val meeting: Meeting,
        val meetingId: Long,
        val creatorUserId: Long,
    )

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    companion object {
        private const val REPRESENTATIVE_PHOTO_URL = "https://example.com/representative.jpg"
        private val FIXED_INSTANT: Instant = Instant.parse("2026-09-30T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
