package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLog
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostLogPhotoLikeServiceIntegrationTest {
    @Autowired
    private lateinit var photoLikeService: PostLogPhotoLikeService

    @Autowired
    private lateinit var photoLikeRepository: PostLogPhotoLikeRepository

    @Autowired
    private lateinit var photoRepository: PostLogPhotoRepository

    @Autowired
    private lateinit var postLogRepository: PostLogRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        photoLikeRepository.deleteAll()
        photoRepository.deleteAll()
        postLogRepository.deleteAll()
        meetingRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `그룹 멤버가 사진에 좋아요를 등록하면 상태와 개수를 반환한다`() {
        val context = savePhotoContext("AB12CD", "member")

        val result = photoLikeService.likePhoto(context.meetingId, context.photoId, context.userId)

        assertEquals(context.photoId, result.photoId)
        assertTrue(result.likedByMe)
        assertEquals(1L, result.likeCount)
        assertEquals(1L, photoLikeRepository.count())
    }

    @Test
    fun `동일한 좋아요 등록을 반복해도 한 건만 유지한다`() {
        val context = savePhotoContext("AB12CD", "member")

        val firstResult = photoLikeService.likePhoto(context.meetingId, context.photoId, context.userId)
        val secondResult = photoLikeService.likePhoto(context.meetingId, context.photoId, context.userId)

        assertTrue(firstResult.likedByMe)
        assertEquals(1L, firstResult.likeCount)
        assertTrue(secondResult.likedByMe)
        assertEquals(1L, secondResult.likeCount)
        assertEquals(1L, photoLikeRepository.count())
    }

    @Test
    fun `좋아요를 해제하면 본인 좋아요만 삭제하고 전체 개수를 반환한다`() {
        val context = savePhotoContext("AB12CD", "member")
        val otherMember = saveGroupMember(context.group, "other-member")
        photoLikeRepository.saveAndFlush(PostLogPhotoLike.create(context.photo, context.groupMember))
        photoLikeRepository.saveAndFlush(PostLogPhotoLike.create(context.photo, otherMember))

        val result = photoLikeService.unlikePhoto(context.meetingId, context.photoId, context.userId)

        assertFalse(result.likedByMe)
        assertEquals(1L, result.likeCount)
        assertEquals(1L, photoLikeRepository.count())
    }

    @Test
    fun `등록하지 않은 좋아요를 해제해도 성공하고 기존 개수를 유지한다`() {
        val context = savePhotoContext("AB12CD", "member")

        val firstResult = photoLikeService.unlikePhoto(context.meetingId, context.photoId, context.userId)
        val secondResult = photoLikeService.unlikePhoto(context.meetingId, context.photoId, context.userId)

        assertFalse(firstResult.likedByMe)
        assertEquals(0L, firstResult.likeCount)
        assertFalse(secondResult.likedByMe)
        assertEquals(0L, secondResult.likeCount)
        assertEquals(0L, photoLikeRepository.count())
    }

    @Test
    fun `다른 만남에 속한 사진에는 좋아요를 등록할 수 없다`() {
        val context = savePhotoContext("AB12CD", "member")
        val otherContext = savePhotoContext("EF34GH", "other-group-member")

        val exception =
            assertFailsWith<BusinessException> {
                photoLikeService.likePhoto(context.meetingId, otherContext.photoId, context.userId)
            }

        assertEquals(PostLogErrorCode.POST_LOG_PHOTO_NOT_FOUND, exception.errorCode)
        assertEquals(0L, photoLikeRepository.count())
    }

    @Test
    fun `그룹 멤버가 아닌 사용자는 사진 좋아요를 등록하거나 해제할 수 없다`() {
        val context = savePhotoContext("AB12CD", "member")
        val outsider = saveCompletedUser("outsider")
        val outsiderId = requireNotNull(outsider.id)

        val likeException =
            assertFailsWith<BusinessException> {
                photoLikeService.likePhoto(context.meetingId, context.photoId, outsiderId)
            }
        val unlikeException =
            assertFailsWith<BusinessException> {
                photoLikeService.unlikePhoto(context.meetingId, context.photoId, outsiderId)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, likeException.errorCode)
        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, unlikeException.errorCode)
        assertEquals(0L, photoLikeRepository.count())
    }

    private fun savePhotoContext(
        inviteCode: String,
        userKey: String,
    ): PhotoContext {
        val group = groupRepository.save(createGroup(inviteCode, userKey))
        val groupMember = saveGroupMember(group, userKey)
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, groupMember, userKey))
        val meetingId = requireNotNull(meeting.id)
        val postLog = postLogRepository.saveAndFlush(PostLog.create(meeting))
        val photo =
            photoRepository.saveAndFlush(
                PostLogPhoto.create(
                    postLog = postLog,
                    uploader = groupMember,
                    objectKey = PostLogPhotoObjectKey.create(meetingId, "post-logs/$meetingId/photos/$userKey.jpg"),
                    capturedAt = OffsetDateTime.parse("2026-09-28T14:37:21+09:00"),
                ),
            )

        return PhotoContext(
            group = group,
            groupMember = groupMember,
            photo = photo,
            meetingId = meetingId,
            photoId = requireNotNull(photo.id),
            userId = requireNotNull(groupMember.user.id),
        )
    }

    private fun createGroup(
        inviteCode: String,
        userKey: String,
    ): Group =
        Group.create(
            name = "${userKey.take(GROUP_NAME_KEY_MAX_LENGTH)} 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun createMeeting(
        group: Group,
        creator: GroupMember,
        userKey: String,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "${userKey.take(MEETING_NAME_KEY_MAX_LENGTH)} 만남",
            location = null,
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
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

    private data class PhotoContext(
        val group: Group,
        val groupMember: GroupMember,
        val photo: PostLogPhoto,
        val meetingId: Long,
        val photoId: Long,
        val userId: Long,
    )

    companion object {
        private const val GROUP_NAME_KEY_MAX_LENGTH = 15
        private const val MEETING_NAME_KEY_MAX_LENGTH = 12
        private const val NICKNAME_MAX_LENGTH = 8
        private const val NAME_MAX_LENGTH = 10

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
