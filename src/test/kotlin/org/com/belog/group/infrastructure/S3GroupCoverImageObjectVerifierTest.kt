package org.com.belog.group.infrastructure

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadata
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.model.S3Exception
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class S3GroupCoverImageObjectVerifierTest {
    private val s3ObjectMetadataProvider = mock(S3ObjectMetadataProvider::class.java)
    private val verifier = S3GroupCoverImageObjectVerifier(s3ObjectMetadataProvider)

    @Test
    fun `S3에 존재하는 올바른 그룹 커버 이미지 객체를 검증한다`() {
        val objectKey = objectKey("image.webp")
        val metadata = S3ObjectMetadata(contentType = "image/webp", contentLength = 524_288L)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        verifier.verify(objectKey)

        verify(s3ObjectMetadataProvider).get(objectKey.value)
    }

    @Test
    fun `S3에 그룹 커버 이미지 객체가 없으면 거부한다`() {
        val objectKey = objectKey("image.webp")
        val s3Exception = S3Exception.builder().statusCode(404).build()
        val notFoundException = S3ObjectNotFoundException(objectKey.value, s3Exception)
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenThrow(notFoundException)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(GroupErrorCode.COVER_IMAGE_NOT_FOUND, exception.errorCode)
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

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `파일 크기가 제한을 초과하면 거부한다`() {
        val objectKey = objectKey("image.webp")
        val metadata =
            S3ObjectMetadata(
                contentType = "image/webp",
                contentLength = GroupCoverImageFormat.MAX_FILE_SIZE_BYTES + 1,
            )
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenReturn(metadata)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey)
            }

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
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

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `S3 조회 중 404가 아닌 오류는 그대로 전파한다`() {
        val objectKey = objectKey("image.webp")
        val s3Exception = S3Exception.builder().statusCode(503).build()
        `when`(s3ObjectMetadataProvider.get(objectKey.value)).thenThrow(s3Exception)

        val exception =
            assertFailsWith<S3Exception> {
                verifier.verify(objectKey)
            }

        assertSame(s3Exception, exception)
    }

    private fun objectKey(fileName: String): GroupCoverImageObjectKey = GroupCoverImageObjectKey.create(15L, "group-covers/15/$fileName")
}
