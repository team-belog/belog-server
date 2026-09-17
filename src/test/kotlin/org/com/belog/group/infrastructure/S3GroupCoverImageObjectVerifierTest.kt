package org.com.belog.group.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class S3GroupCoverImageObjectVerifierTest {
    private val properties =
        S3StorageProperties(
            bucket = "belog-test-storage",
            region = "ap-northeast-2",
            presignExpiration = Duration.ofMinutes(5),
        )
    private val s3Client = mock(S3Client::class.java)
    private val verifier = S3GroupCoverImageObjectVerifier(s3Client, properties)

    @Test
    fun `S3에 존재하는 올바른 그룹 커버 이미지 객체를 검증한다`() {
        val objectKey = objectKey("image.webp")
        val response =
            HeadObjectResponse
                .builder()
                .contentType("image/webp")
                .contentLength(524_288L)
                .build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(response)

        verifier.verify(objectKey)

        val requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest::class.java)
        verify(s3Client).headObject(requestCaptor.capture())
        assertEquals(properties.bucket, requestCaptor.value.bucket())
        assertEquals(objectKey.value, requestCaptor.value.key())
    }

    @Test
    fun `S3에 그룹 커버 이미지 객체가 없으면 거부한다`() {
        val s3Exception = S3Exception.builder().statusCode(404).build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenThrow(s3Exception)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey("image.webp"))
            }

        assertEquals(GroupErrorCode.COVER_IMAGE_NOT_FOUND, exception.errorCode)
        assertSame(s3Exception, exception.cause)
    }

    @Test
    fun `지원하지 않는 Content-Type이면 거부한다`() {
        val response =
            HeadObjectResponse
                .builder()
                .contentType("image/gif")
                .contentLength(1024L)
                .build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(response)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey("image.gif"))
            }

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `파일 크기가 제한을 초과하면 거부한다`() {
        val response =
            HeadObjectResponse
                .builder()
                .contentType("image/webp")
                .contentLength(GroupCoverImageFormat.MAX_FILE_SIZE_BYTES + 1)
                .build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(response)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey("image.webp"))
            }

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `Content-Type과 확장자가 일치하지 않으면 거부한다`() {
        val response =
            HeadObjectResponse
                .builder()
                .contentType("image/png")
                .contentLength(1024L)
                .build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(response)

        val exception =
            assertFailsWith<BusinessException> {
                verifier.verify(objectKey("image.webp"))
            }

        assertEquals(GroupErrorCode.INVALID_COVER_IMAGE_METADATA, exception.errorCode)
    }

    @Test
    fun `S3 조회 중 404가 아닌 오류는 그대로 전파한다`() {
        val s3Exception = S3Exception.builder().statusCode(503).build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenThrow(s3Exception)

        val exception =
            assertFailsWith<S3Exception> {
                verifier.verify(objectKey("image.webp"))
            }

        assertSame(s3Exception, exception)
    }

    private fun objectKey(fileName: String): GroupCoverImageObjectKey = GroupCoverImageObjectKey.create(15L, "group-covers/15/$fileName")
}
