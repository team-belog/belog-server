package org.com.belog.postlog.service

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
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
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostLogCalendarServiceIntegrationTest {
    @Autowired
    private lateinit var calendarService: PostLogCalendarService

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
    fun `요청 월에 종료된 본인의 생성 티켓만 정렬해 달력에 반환한다`() {
        val group = saveGroup()
        val owner = saveGroupMember(group, "owner")
        val otherMember = saveGroupMember(group, "other")
        val firstMeeting = saveMeeting(group, owner, "첫 번째 만남", LocalDate.of(2026, 8, 12))
        val secondMeeting = saveMeeting(group, owner, "두 번째 만남", LocalDate.of(2026, 8, 13))
        val thirdMeeting = saveMeeting(group, owner, "세 번째 만남", LocalDate.of(2026, 8, 13))
        val draftMeeting = saveMeeting(group, owner, "임시저장 만남", LocalDate.of(2026, 8, 14))
        val otherMeeting = saveMeeting(group, otherMember, "다른 사용자 만남", LocalDate.of(2026, 8, 15))
        val deletedMeeting = saveMeeting(group, owner, "삭제된 만남", LocalDate.of(2026, 8, 16))
        val outsideMeeting = saveMeeting(group, owner, "다음 달 만남", LocalDate.of(2026, 9, 1))
        val ownerUserId = checkNotNull(owner.user.id)

        val firstTicket = createTicket(firstMeeting, owner, "첫 번째 추억")
        val secondTicket = createTicket(secondMeeting, owner, "두 번째 추억")
        val thirdTicket = createTicket(thirdMeeting, owner, "세 번째 추억")
        postLogService.saveDraft(checkNotNull(draftMeeting.id), ownerUserId, "임시저장 추억")
        createTicket(otherMeeting, otherMember, "다른 사용자의 추억")
        createTicket(deletedMeeting, owner, "삭제된 만남의 추억")
        createTicket(outsideMeeting, owner, "다음 달 추억")
        deletedMeeting.delete(Instant.parse("2026-09-02T00:00:00Z"))
        meetingRepository.saveAndFlush(deletedMeeting)

        val result = calendarService.getMyTicketCalendar(ownerUserId, YearMonth.of(2026, 8))

        assertEquals(YearMonth.of(2026, 8), result.yearMonth)
        assertEquals(3, result.memoryCount)
        assertEquals(
            listOf(firstTicket.postLogId, secondTicket.postLogId, thirdTicket.postLogId),
            result.tickets.map { ticket -> ticket.postLogId },
        )
        assertEquals(
            listOf(LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 13)),
            result.tickets.map { ticket -> ticket.meetingEndDate },
        )
    }

    @Test
    fun `만남별 좋아요가 가장 많은 사진을 대표 사진으로 반환하고 사진이 없으면 null을 반환한다`() {
        val group = saveGroup()
        val owner = saveGroupMember(group, "owner")
        val firstLiker = saveGroupMember(group, "liker1")
        val secondLiker = saveGroupMember(group, "liker2")
        val photoMeeting = saveMeeting(group, owner, "사진 있는 만남", LocalDate.of(2026, 8, 12))
        val emptyMeeting = saveMeeting(group, owner, "사진 없는 만남", LocalDate.of(2026, 8, 13))
        createTicket(photoMeeting, owner, "사진 있는 추억")
        createTicket(emptyMeeting, owner, "사진 없는 추억")
        val firstPhoto = savePhoto(photoMeeting, owner, "first.jpg", "2026-08-12T10:00:00+09:00")
        val secondPhoto = savePhoto(photoMeeting, owner, "second.jpg", "2026-08-12T11:00:00+09:00")
        photoLikeRepository.saveAllAndFlush(
            listOf(
                PostLogPhotoLike.create(firstPhoto, firstLiker),
                PostLogPhotoLike.create(firstPhoto, secondLiker),
                PostLogPhotoLike.create(secondPhoto, firstLiker),
                PostLogPhotoLike.create(secondPhoto, secondLiker),
            ),
        )
        `when`(objectReadUrlProvider.generateReadUrl(firstPhoto.objectKey)).thenReturn(REPRESENTATIVE_PHOTO_URL)

        val result = calendarService.getMyTicketCalendar(checkNotNull(owner.user.id), YearMonth.of(2026, 8))

        assertEquals(REPRESENTATIVE_PHOTO_URL, result.tickets[0].thumbnailUrl)
        assertNull(result.tickets[1].thumbnailUrl)
    }

    private fun createTicket(
        meeting: Meeting,
        creator: GroupMember,
        memory: String,
    ) = postLogService.createTicket(
        meetingId = checkNotNull(meeting.id),
        userId = checkNotNull(creator.user.id),
        memory = memory,
    )

    private fun saveMeeting(
        group: Group,
        creator: GroupMember,
        name: String,
        endDate: LocalDate,
    ): Meeting {
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = creator,
                    name = name,
                    location = "서울",
                    dateRange = MeetingDateRange(endDate.minusDays(1), endDate),
                    confirmedAt = Instant.parse("2026-07-01T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 7, 1),
                ),
            )
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))
        return meeting
    }

    private fun savePhoto(
        meeting: Meeting,
        uploader: GroupMember,
        fileName: String,
        capturedAt: String,
    ): PostLogPhoto =
        photoRepository.saveAndFlush(
            PostLogPhoto.create(
                meeting = meeting,
                uploader = uploader,
                objectKey =
                    PostLogPhotoObjectKey.create(
                        checkNotNull(meeting.id),
                        "post-logs/${meeting.id}/photos/$fileName",
                    ),
                capturedAt = OffsetDateTime.parse(capturedAt),
            ),
        )

    private fun saveGroup(): Group =
        groupRepository.saveAndFlush(
            Group.create(
                name = "주말 여행 모임",
                coverImageObjectKey = null,
                inviteCode = InviteCode.create("AB12CD"),
            ),
        )

    private fun saveGroupMember(
        group: Group,
        userKey: String,
    ): GroupMember =
        groupMemberRepository.saveAndFlush(
            GroupMember.createMember(group, saveCompletedUser(userKey)),
        )

    private fun saveCompletedUser(userKey: String): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$userKey@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = userKey,
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
            completedAt = Instant.parse("2026-07-01T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    companion object {
        private const val REPRESENTATIVE_PHOTO_URL = "https://example.com/representative.jpg"

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
