package org.com.belog.user.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.user.domain.ProfileImageObjectKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S3ProfileImageReadUrlProviderTest {
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
    private val provider = S3ProfileImageReadUrlProvider(presigner, properties)

    @AfterEach
    fun closePresigner() {
        presigner.close()
    }

    @Test
    fun `프로필 이미지 object key로 5분 동안 유효한 GET URL을 생성한다`() {
        val objectKey = ProfileImageObjectKey.create(15L, "users/15/profile/image.webp")

        val result = provider.generateReadUrl(objectKey)

        val decodedUrl = URLDecoder.decode(result, StandardCharsets.UTF_8)
        assertTrue(result.startsWith("https://belog-test-storage.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp"))
        assertTrue(decodedUrl.contains("X-Amz-Expires=300"))
        assertTrue(decodedUrl.contains("X-Amz-SignedHeaders=host"))
        assertEquals("users/15/profile/image.webp", objectKey.value)
    }
}
