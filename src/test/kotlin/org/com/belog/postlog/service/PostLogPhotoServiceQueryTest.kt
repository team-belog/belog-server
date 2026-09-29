package org.com.belog.postlog.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoLike
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.infrastructure.S3PostLogPhotoObjectVerifier
import org.com.belog.postlog.infrastructure.S3PostLogPhotoUploadUrlProvider
import org.com.belog.postlog.repository.PostLogPhotoLikeRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Base64
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    PostLogPhotoService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class PostLogPhotoServiceQueryTest {
    @Autowired
    private lateinit var photoService: PostLogPhotoService

    @Autowired
    private lateinit var photoLikeRepository: PostLogPhotoLikeRepository

    @Autowired
    private lateinit var photoRepository: PostLogPhotoRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var uploadUrlProvider: S3PostLogPhotoUploadUrlProvider

    @MockitoBean
    private lateinit var objectVerifier: S3PostLogPhotoObjectVerifier

    @MockitoBean
    private lateinit var registrationService: PostLogPhotoRegistrationService

    @MockitoBean
    private lateinit var objectReadUrlProvider: S3ObjectReadUrlProvider

    @BeforeEach
    fun setUpReadUrls() {
        `when`(objectReadUrlProvider.generateReadUrl(anyString())).thenAnswer { invocation ->
            "https://storage.example/${invocation.getArgument<String>(0)}"
        }
    }

    @Test
    fun `사진을 촬영 시각과 사진 ID 오름차순으로 조회한다`() {
        val context = saveMeetingContext()
        val latePhoto = savePhoto(context, "late.jpg", "2026-09-28T13:00:00+09:00")
        val firstPhoto = savePhoto(context, "first.jpg", "2026-09-28T12:00:00+09:00")
        val secondPhoto = savePhoto(context, "second.jpg", "2026-09-28T12:00:00+09:00")

        val result = photoService.getPhotos(context.meetingId, context.userId, cursor = null)

        assertEquals(
            listOf(requireNotNull(firstPhoto.id), requireNotNull(secondPhoto.id), requireNotNull(latePhoto.id)),
            result.items.map { item -> item.photoId },
        )
    }

    @Test
    fun `동일한 촬영 시각에서 페이지가 나뉘어도 사진을 누락하거나 중복하지 않는다`() {
        val context = saveMeetingContext()
        val photos =
            (1..5).map { sequence ->
                savePhoto(context, "same-time-$sequence.jpg", "2026-09-28T12:00:00+09:00")
            }

        val firstPage = photoService.getPhotos(context.meetingId, context.userId, cursor = null, size = 2)
        val secondPage = photoService.getPhotos(context.meetingId, context.userId, cursor = firstPage.nextCursor, size = 2)
        val thirdPage = photoService.getPhotos(context.meetingId, context.userId, cursor = secondPage.nextCursor, size = 2)
        val actualPhotoIds =
            listOf(firstPage, secondPage, thirdPage)
                .flatMap { page -> page.items }
                .map { item -> item.photoId }

        assertEquals(photos.map { photo -> requireNotNull(photo.id) }, actualPhotoIds)
        assertEquals(actualPhotoIds.size, actualPhotoIds.distinct().size)
        assertTrue(firstPage.hasNext)
        assertNotNull(firstPage.nextCursor)
        assertTrue(secondPage.hasNext)
        assertNotNull(secondPage.nextCursor)
        assertFalse(thirdPage.hasNext)
        assertNull(thirdPage.nextCursor)
    }

    @Test
    fun `사진 수가 조회 개수와 같으면 다음 페이지가 없다`() {
        val context = saveMeetingContext()
        savePhoto(context, "first.jpg", "2026-09-28T12:00:00+09:00")
        savePhoto(context, "second.jpg", "2026-09-28T13:00:00+09:00")

        val result = photoService.getPhotos(context.meetingId, context.userId, cursor = null, size = 2)

        assertEquals(2, result.items.size)
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    fun `등록된 사진이 없으면 빈 목록을 반환한다`() {
        val context = saveMeetingContext()

        val result = photoService.getPhotos(context.meetingId, context.userId, cursor = null)

        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    fun `사진별 전체 좋아요 수와 로그인 사용자의 좋아요 여부를 반환한다`() {
        val context = saveMeetingContext()
        val otherMember = saveGroupMember(context.group, "other-member")
        val thirdMember = saveGroupMember(context.group, "third-member")
        val likedByMePhoto = savePhoto(context, "liked-by-me.jpg", "2026-09-28T12:00:00+09:00")
        val likedByOthersPhoto = savePhoto(context, "liked-by-others.jpg", "2026-09-28T13:00:00+09:00")
        val unlikedPhoto = savePhoto(context, "unliked.jpg", "2026-09-28T14:00:00+09:00")
        photoLikeRepository.saveAllAndFlush(
            listOf(
                PostLogPhotoLike.create(likedByMePhoto, context.groupMember),
                PostLogPhotoLike.create(likedByMePhoto, otherMember),
                PostLogPhotoLike.create(likedByMePhoto, thirdMember),
                PostLogPhotoLike.create(likedByOthersPhoto, otherMember),
            ),
        )

        val result = photoService.getPhotos(context.meetingId, context.userId, cursor = null)

        assertEquals(listOf(3L, 1L, 0L), result.items.map { item -> item.likeCount })
        assertEquals(listOf(true, false, false), result.items.map { item -> item.likedByMe })
        assertEquals(requireNotNull(unlikedPhoto.id), result.items.last().photoId)
    }

    @Test
    fun `그룹 멤버가 아닌 사용자는 사진을 조회할 수 없다`() {
        val context = saveMeetingContext()
        val outsider = saveCompletedUser("outsider")

        val exception =
            assertFailsWith<BusinessException> {
                photoService.getPhotos(
                    meetingId = context.meetingId,
                    userId = requireNotNull(outsider.id),
                    cursor = null,
                )
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 만남의 사진은 조회할 수 없다`() {
        val exception =
            assertFailsWith<BusinessException> {
                photoService.getPhotos(meetingId = Long.MAX_VALUE, userId = Long.MAX_VALUE, cursor = null)
            }

        assertEquals(MeetingErrorCode.MEETING_NOT_FOUND, exception.errorCode)
    }

    @ParameterizedTest
    @MethodSource("invalidCursors")
    fun `올바르지 않은 사진 커서는 거부한다`(cursor: String) {
        val context = saveMeetingContext()

        val exception =
            assertFailsWith<BusinessException> {
                photoService.getPhotos(context.meetingId, context.userId, cursor)
            }

        assertEquals(PostLogErrorCode.INVALID_PHOTO_CURSOR, exception.errorCode)
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
        val groupMember = saveGroupMember(group, "member")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = groupMember,
                    name = "광주 여행",
                    location = null,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        return MeetingContext(
            group = group,
            groupMember = groupMember,
            meeting = meeting,
            meetingId = requireNotNull(meeting.id),
            userId = requireNotNull(groupMember.user.id),
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
                uploader = context.groupMember,
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
                    providerUserId = "$userKey-subject",
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = userKey.take(NICKNAME_MAX_LENGTH),
            name = userKey.take(NAME_MAX_LENGTH),
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
        val groupMember: GroupMember,
        val meeting: Meeting,
        val meetingId: Long,
        val userId: Long,
    )

    companion object {
        private const val NICKNAME_MAX_LENGTH = 8
        private const val NAME_MAX_LENGTH = 10

        @JvmStatic
        fun invalidCursors(): Stream<String> =
            Stream.of(
                "not-base64!",
                encodeCursorValue("missing-delimiter"),
                encodeCursorValue("not-an-instant|1"),
                encodeCursorValue("2026-09-28T03:00:00Z|not-a-number"),
                encodeCursorValue("2026-09-28T03:00:00Z|0"),
            )

        private fun encodeCursorValue(value: String): String =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }
}
