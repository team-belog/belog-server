package org.com.belog.postlog.infrastructure

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadata
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.config.PostLogPhotoVerificationProperties
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class S3PostLogPhotoObjectVerifierTest {
    private val metadataProvider = mock(S3ObjectMetadataProvider::class.java)
    private lateinit var verifier: S3PostLogPhotoObjectVerifier

    @AfterEach
    fun shutDownVerifier() {
        if (::verifier.isInitialized) {
            verifier.shutdown()
        }
    }

    @Test
    fun `모든 S3 객체가 제한 시간 안에 검증되면 성공한다`() {
        verifier = createVerifier(maxConcurrentRequests = 2, queueCapacity = 2, timeout = Duration.ofSeconds(1))
        `when`(metadataProvider.get(anyString())).thenReturn(validMetadata())

        verifier.verifyAll(listOf(objectKey("first.jpg"), objectKey("second.jpg")))
    }

    @Test
    fun `일괄 검증 제한 시간을 초과하면 재시도 가능한 오류를 반환한다`() {
        val releaseRequest = CountDownLatch(1)
        verifier = createVerifier(maxConcurrentRequests = 1, queueCapacity = 1, timeout = Duration.ofMillis(100))
        `when`(metadataProvider.get(anyString())).thenAnswer {
            releaseRequest.await()
            validMetadata()
        }

        try {
            val exception =
                assertFailsWith<BusinessException> {
                    verifier.verifyAll(listOf(objectKey("photo.jpg")))
                }

            assertEquals(PostLogErrorCode.PHOTO_VERIFICATION_UNAVAILABLE, exception.errorCode)
        } finally {
            releaseRequest.countDown()
        }
    }

    @Test
    fun `검증 실행기 대기열이 가득 차면 요청을 거부한다`() {
        val releaseRequest = CountDownLatch(1)
        verifier = createVerifier(maxConcurrentRequests = 1, queueCapacity = 1, timeout = Duration.ofSeconds(1))
        `when`(metadataProvider.get(anyString())).thenAnswer {
            releaseRequest.await()
            validMetadata()
        }

        try {
            val exception =
                assertFailsWith<BusinessException> {
                    verifier.verifyAll(
                        listOf(
                            objectKey("first.jpg"),
                            objectKey("second.jpg"),
                            objectKey("third.jpg"),
                        ),
                    )
                }

            assertEquals(PostLogErrorCode.PHOTO_VERIFICATION_UNAVAILABLE, exception.errorCode)
        } finally {
            releaseRequest.countDown()
        }
    }

    private fun createVerifier(
        maxConcurrentRequests: Int,
        queueCapacity: Int,
        timeout: Duration,
    ): S3PostLogPhotoObjectVerifier =
        S3PostLogPhotoObjectVerifier(
            s3ObjectMetadataProvider = metadataProvider,
            properties =
                PostLogPhotoVerificationProperties(
                    maxConcurrentHeadRequests = maxConcurrentRequests,
                    queueCapacity = queueCapacity,
                    timeout = timeout,
                ),
        )

    private fun objectKey(fileName: String): PostLogPhotoObjectKey =
        PostLogPhotoObjectKey.create(MEETING_ID, "post-logs/$MEETING_ID/photos/$fileName")

    private fun validMetadata(): S3ObjectMetadata =
        S3ObjectMetadata(
            contentType = "image/jpeg",
            contentLength = 1024L,
        )

    companion object {
        private const val MEETING_ID = 7L
    }
}
