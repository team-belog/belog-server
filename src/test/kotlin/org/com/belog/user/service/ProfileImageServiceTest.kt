package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.ProfileImageUpload
import org.com.belog.user.infrastructure.S3ProfileImageUploadUrlProvider
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileImageServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val profileImageUploadUrlProvider = mock(S3ProfileImageUploadUrlProvider::class.java)
    private val s3ObjectReadUrlProvider = mock(S3ObjectReadUrlProvider::class.java)
    private val service = ProfileImageService(userRepository, profileImageUploadUrlProvider, s3ObjectReadUrlProvider)

    @Test
    fun `지원하는 이미지 형식이면 업로드 URL을 발급한다`() {
        val expected =
            ProfileImageUpload(
                objectKey = "users/15/profile/image-id.webp",
                uploadUrl = "https://example.com/upload",
                contentType = "image/webp",
                contentLength = 1024L,
                expiresAt = Instant.parse("2026-09-14T14:05:00Z"),
            )
        `when`(userRepository.existsById(15L)).thenReturn(true)
        `when`(profileImageUploadUrlProvider.issueUploadUrl(15L, ProfileImageFormat.WEBP, 1024L)).thenReturn(expected)

        val result = service.issueUploadUrl(15L, "IMAGE/WEBP", 1024L)

        assertEquals(expected, result)
        verify(profileImageUploadUrlProvider).issueUploadUrl(15L, ProfileImageFormat.WEBP, 1024L)
    }

    @Test
    fun `지원하지 않는 이미지 형식은 거부한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                service.issueUploadUrl(15L, "image/gif", 1024L)
            }

        assertEquals(UserErrorCode.UNSUPPORTED_PROFILE_IMAGE_TYPE, exception.errorCode)
        verifyNoInteractions(userRepository, profileImageUploadUrlProvider)
    }

    @Test
    fun `존재하지 않는 사용자는 업로드 URL을 발급받을 수 없다`() {
        `when`(userRepository.existsById(15L)).thenReturn(false)

        val exception =
            assertFailsWith<BusinessException> {
                service.issueUploadUrl(15L, "image/webp", 1024L)
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(profileImageUploadUrlProvider)
    }

    @Test
    fun `프로필 이미지 object key로 조회 URL을 생성한다`() {
        val objectKey = ProfileImageObjectKey.create(15L, "users/15/profile/image.webp")
        `when`(s3ObjectReadUrlProvider.generateReadUrl(objectKey.value)).thenReturn("https://example.com/profile")

        val result = service.generateReadUrl(15L, objectKey.value)

        assertEquals("https://example.com/profile", result)
        verify(s3ObjectReadUrlProvider).generateReadUrl(objectKey.value)
    }

    @Test
    fun `프로필 이미지 object key가 없으면 조회 URL을 생성하지 않는다`() {
        val result = service.generateReadUrl(15L, null)

        assertEquals(null, result)
        verifyNoInteractions(s3ObjectReadUrlProvider)
    }
}
