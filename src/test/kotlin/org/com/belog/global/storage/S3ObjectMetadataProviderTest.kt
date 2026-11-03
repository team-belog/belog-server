package org.com.belog.global.storage

import org.com.belog.global.config.S3StorageProperties
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class S3ObjectMetadataProviderTest {
    private val s3Client = mock(S3Client::class.java)
    private val provider =
        S3ObjectMetadataProvider(
            s3Client = s3Client,
            properties =
                S3StorageProperties(
                    bucket = "belog-test-storage",
                    region = "ap-northeast-2",
                    presignExpiration = Duration.ofMinutes(5),
                    connectionTimeout = Duration.ofSeconds(2),
                    socketTimeout = Duration.ofSeconds(5),
                    apiCallAttemptTimeout = Duration.ofSeconds(5),
                    apiCallTimeout = Duration.ofSeconds(10),
                ),
        )

    @Test
    fun `S3가 404를 반환하면 객체 없음 예외로 변환한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenThrow(S3Exception.builder().statusCode(404).build())

        val exception = assertFailsWith<S3ObjectNotFoundException> { provider.get(OBJECT_KEY) }

        assertEquals(OBJECT_KEY, exception.objectKey)
    }

    @Test
    fun `S3가 404가 아닌 오류를 반환하면 객체 확인 불가 예외로 변환한다`() {
        val cause = S3Exception.builder().statusCode(403).build()
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenThrow(cause)

        val exception = assertFailsWith<S3ObjectUnavailableException> { provider.get(OBJECT_KEY) }

        assertSame(cause, exception.cause)
    }

    @Test
    fun `S3 요청을 완료하지 못하면 객체 확인 불가 예외로 변환한다`() {
        val cause = SdkClientException.create("Unable to load credentials")
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenThrow(cause)

        val exception = assertFailsWith<S3ObjectUnavailableException> { provider.get(OBJECT_KEY) }

        assertSame(cause, exception.cause)
    }

    companion object {
        private const val OBJECT_KEY = "bill-log/receipts/7/15/receipt.jpg"
    }
}
