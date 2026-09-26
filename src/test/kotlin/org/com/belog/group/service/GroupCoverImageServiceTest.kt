package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.GroupCoverImageUpload
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.infrastructure.S3GroupCoverImageObjectVerifier
import org.com.belog.group.infrastructure.S3GroupCoverImageUploadUrlProvider
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupCoverImageServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val groupCoverImageUploadUrlProvider = mock(S3GroupCoverImageUploadUrlProvider::class.java)
    private val groupRepository = mock(GroupRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val groupCoverImageObjectVerifier = mock(S3GroupCoverImageObjectVerifier::class.java)
    private val groupCoverImageUpdateService = mock(GroupCoverImageUpdateService::class.java)
    private val service =
        GroupCoverImageService(
            userRepository,
            groupCoverImageUploadUrlProvider,
            groupRepository,
            groupMemberRepository,
            groupCoverImageObjectVerifier,
            groupCoverImageUpdateService,
        )

    @Test
    fun `온보딩을 완료한 사용자가 지원하는 이미지 형식을 요청하면 업로드 URL을 발급한다`() {
        val user = mock(User::class.java)
        val expected =
            GroupCoverImageUpload(
                objectKey = "group-covers/15/image-id.webp",
                uploadUrl = "https://example.com/upload",
                contentType = "image/webp",
                contentLength = 1024L,
                expiresAt = Instant.parse("2026-09-17T03:00:00Z"),
            )
        `when`(user.isOnboardingCompleted).thenReturn(true)
        `when`(userRepository.findById(15L)).thenReturn(Optional.of(user))
        `when`(
            groupCoverImageUploadUrlProvider.issueUploadUrl(
                15L,
                GroupCoverImageFormat.WEBP,
                1024L,
            ),
        ).thenReturn(expected)

        val result = service.issueUploadUrl(15L, " IMAGE/WEBP ", 1024L)

        assertEquals(expected, result)
        verify(groupCoverImageUploadUrlProvider).issueUploadUrl(15L, GroupCoverImageFormat.WEBP, 1024L)
    }

    @Test
    fun `지원하지 않는 이미지 형식은 거부한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service.issueUploadUrl(15L, "image/gif", 1024L)
            }

        assertEquals(GroupErrorCode.UNSUPPORTED_COVER_IMAGE_TYPE, exception.errorCode)
        verifyNoInteractions(userRepository, groupCoverImageUploadUrlProvider)
    }

    @Test
    fun `파일 크기가 올바르지 않으면 거부한다`() {
        listOf(0L, -1L, GroupCoverImageFormat.MAX_FILE_SIZE_BYTES + 1).forEach { fileSize ->
            val exception =
                assertFailsWith<BusinessException> {
                    service.issueUploadUrl(15L, "image/webp", fileSize)
                }

            assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_SIZE, exception.errorCode)
        }

        verifyNoInteractions(userRepository, groupCoverImageUploadUrlProvider)
    }

    @Test
    fun `존재하지 않는 사용자는 업로드 URL을 발급받을 수 없다`() {
        `when`(userRepository.findById(15L)).thenReturn(Optional.empty())

        val exception =
            assertFailsWith<BusinessException> {
                service.issueUploadUrl(15L, "image/webp", 1024L)
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(groupCoverImageUploadUrlProvider)
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자는 업로드 URL을 발급받을 수 없다`() {
        val user = mock(User::class.java)
        `when`(userRepository.findById(15L)).thenReturn(Optional.of(user))

        val exception =
            assertFailsWith<BusinessException> {
                service.issueUploadUrl(15L, "image/webp", 1024L)
            }

        assertEquals(GroupErrorCode.ONBOARDING_REQUIRED, exception.errorCode)
        verifyNoInteractions(groupCoverImageUploadUrlProvider)
    }

    @Test
    fun `OWNER는 검증된 커버 이미지로 그룹 커버 이미지를 변경한다`() {
        val objectKey = GroupCoverImageObjectKey.create(15L, "group-covers/15/image.webp")
        val owner = mock(GroupMember::class.java)
        `when`(groupRepository.existsById(1L)).thenReturn(true)
        `when`(groupMemberRepository.findByGroupIdAndUserId(1L, 15L)).thenReturn(owner)
        `when`(owner.role).thenReturn(GroupRole.OWNER)

        service.updateCoverImage(1L, 15L, objectKey)

        verify(groupCoverImageObjectVerifier).verify(objectKey)
        verify(groupCoverImageUpdateService).update(1L, objectKey)
    }

    @Test
    fun `MEMBER는 그룹 커버 이미지를 변경할 수 없다`() {
        val objectKey = GroupCoverImageObjectKey.create(15L, "group-covers/15/image.webp")
        val member = mock(GroupMember::class.java)
        `when`(groupRepository.existsById(1L)).thenReturn(true)
        `when`(groupMemberRepository.findByGroupIdAndUserId(1L, 15L)).thenReturn(member)
        `when`(member.role).thenReturn(GroupRole.MEMBER)

        val exception =
            assertFailsWith<BusinessException> {
                service.updateCoverImage(1L, 15L, objectKey)
            }

        assertEquals(GroupErrorCode.GROUP_OWNER_REQUIRED, exception.errorCode)
        verifyNoInteractions(groupCoverImageObjectVerifier, groupCoverImageUpdateService)
    }
}
