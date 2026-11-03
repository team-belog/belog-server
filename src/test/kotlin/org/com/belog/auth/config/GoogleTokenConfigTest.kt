package org.com.belog.auth.config

import org.junit.jupiter.api.Test
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GoogleTokenConfigTest {
    private val config = GoogleTokenConfig()

    @Test
    fun `Google 공개 키 조회에 연결 및 응답 제한 시간을 설정한다`() {
        val restOperations = config.googleJwkRestOperations()
        val restTemplate = assertIs<RestTemplate>(restOperations)
        val requestFactory = assertIs<SimpleClientHttpRequestFactory>(restTemplate.requestFactory)

        assertEquals(3_000, ReflectionTestUtils.getField(requestFactory, "connectTimeout"))
        assertEquals(5_000, ReflectionTestUtils.getField(requestFactory, "readTimeout"))
    }
}
