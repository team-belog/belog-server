package org.com.belog.group.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.group.domain.GroupCoverImageFormat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S3GroupCoverImageUploadUrlProviderTest {
    private val properties =
        S3StorageProperties(
            bucket = "belog-test-storage",
            region = "ap-northeast-2",
            presignExpiration = Duration.ofMinutes(5),
        )
    private val presigner =
        S3Presigner
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key"),
                ),
            ).build()
    private val provider = S3GroupCoverImageUploadUrlProvider(presigner, properties)

    @AfterEach
    fun closePresigner() {
        presigner.close()
    }

    @Test
    fun `사용자별 그룹 커버 경로에 5분 동안 유효한 PUT URL을 발급한다`() {
        val earliestExpiration = Instant.now().plus(properties.presignExpiration).minusSeconds(1)
        val result = provider.issueUploadUrl(15L, GroupCoverImageFormat.WEBP, 524_288L)
        val latestExpiration = Instant.now().plus(properties.presignExpiration).plusSeconds(1)
        val decodedQuery = URLDecoder.decode(result.uploadUrl, StandardCharsets.UTF_8)

        assertTrue(result.objectKey.matches(Regex("group-covers/15/[0-9a-f-]{36}\\.webp")))
        assertTrue(result.uploadUrl.startsWith("https://belog-test-storage.s3.ap-northeast-2.amazonaws.com/"))
        assertTrue(decodedQuery.contains("X-Amz-Expires=300"))
        assertTrue(decodedQuery.contains("X-Amz-SignedHeaders=content-length;content-type;host"))
        assertEquals("image/webp", result.contentType)
        assertEquals(524_288L, result.contentLength)
        assertTrue(result.expiresAt.isAfter(earliestExpiration))
        assertTrue(result.expiresAt.isBefore(latestExpiration))
    }
}
