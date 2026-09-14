package org.com.belog.global.config

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class S3StoragePropertiesTest {
    @Test
    fun `Presigned URL 만료 시간이 15분을 초과하면 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            S3StorageProperties(
                bucket = "belog-test-storage",
                region = "ap-northeast-2",
                presignExpiration = Duration.ofMinutes(16),
            )
        }
    }

    @Test
    fun `빈 S3 버킷 이름을 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            S3StorageProperties(
                bucket = " ",
                region = "ap-northeast-2",
                presignExpiration = Duration.ofMinutes(5),
            )
        }
    }
}
