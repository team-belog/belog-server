package org.com.belog.user.infrastructure

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadata
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageObjectKey
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.model.S3Exception
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class S3ProfileImageObjectVerifierTest {
    private val s3ObjectMetadataProvider = mock(S3ObjectMetadataProvider::class.java)
    private val verifier = S3ProfileImageObjectVerifier(s3ObjectMetadataProvider)

    @Test
    fun `S3에 존재하는 올바른 프로필 이미지 객체를 검증한다`() {
        val objectKey = objectKey("image.webp")
        val metadata = S3ObjectMetadata(contentType = "image/webp", contentLength = 524_288L)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        verifier.verify(objectKey)

        verify(s3ObjectMetadataProvider).get(objectKey.value)
    }

    @Test
    fun `S3에 프로필 이미지 객체가 없으면 거부한다`() {
        val objectKey = objectKey("image.webp")
        val s3Exception = S3Exception.builder().statusCode(404).build()
        val notFoundException = S3ObjectNotFoundException(objectKey.value, s3Exception)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenThrow(notFoundException)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(UserErrorCode.PROFILE_IMAGE_NOT_FOUND, exception.errorCode)
        assertSame(notFoundException, exception.cause)
    }

    @Test
    fun `지원하지 않는 Content-Type이면 거부한다`() {
        val objectKey = objectKey("image.gif")
        val metadata = S3ObjectMetadata(contentType = "image/gif", contentLength = 1024L)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(UserErrorCode.INVALID_PROFILE_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `파일 크기가 제한을 초과하면 거부한다`() {
        val objectKey = objectKey("image.webp")
        val metadata =
            S3ObjectMetadata(
                contentType = "image/webp",
                contentLength = ProfileImageFormat.MAX_FILE_SIZE_BYTES + 1,
            )
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(UserErrorCode.INVALID_PROFILE_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `Content-Type과 확장자가 일치하지 않으면 거부한다`() {
        val objectKey = objectKey("image.webp")
        val metadata = S3ObjectMetadata(contentType = "image/png", contentLength = 1024L)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(UserErrorCode.INVALID_PROFILE_IMAGE_METADATA, exception.errorCode)
    }

    private fun objectKey(fileName: String): ProfileImageObjectKey = ProfileImageObjectKey.create(15L, "users/15/profile/$fileName")
}
