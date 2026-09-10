package org.com.belog.auth.infrastructure

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.config.GoogleAuthProperties
import org.com.belog.global.error.BusinessException
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoogleAuthorizationCodeClientTest {
    private val restClientBuilder = RestClient.builder().baseUrl("https://oauth2.googleapis.com")
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val client = GoogleAuthorizationCodeClient(restClientBuilder.build(), properties)

    @Test
    fun `인가 코드를 Google ID Token으로 교환한다`() {
        server
            .expect(requestTo("https://oauth2.googleapis.com/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().string(
                    allOf(
                        containsString("code=google-authorization-code"),
                        containsString("client_id=test-google-client-id"),
                        containsString("client_secret=test-google-client-secret"),
                        containsString("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Foauth%2Fgoogle%2Fcallback"),
                        containsString("grant_type=authorization_code"),
                    ),
                ),
            ).andRespond(withSuccess("""{"id_token":"google-id-token"}""", MediaType.APPLICATION_JSON))

        val idToken =
            client.exchangeForIdToken(
                "google-authorization-code",
                "http://localhost:3000/oauth/google/callback",
            )

        assertEquals("google-id-token", idToken)
        server.verify()
    }

    @Test
    fun `Google이 인가 코드를 거부하면 인증 실패로 처리한다`() {
        server
            .expect(requestTo("https://oauth2.googleapis.com/token"))
            .andRespond(withBadRequest())

        val exception =
            assertFailsWith<BusinessException> {
                client.exchangeForIdToken(
                    "invalid-authorization-code",
                    "http://localhost:3000/oauth/google/callback",
                )
            }

        assertEquals(AuthErrorCode.INVALID_GOOGLE_AUTHORIZATION_CODE, exception.errorCode)
    }

    @Test
    fun `허용되지 않은 리디렉션 URI이면 요청을 거부한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                client.exchangeForIdToken(
                    "google-authorization-code",
                    "https://attacker.example/oauth/google/callback",
                )
            }

        assertEquals(AuthErrorCode.INVALID_GOOGLE_REDIRECT_URI, exception.errorCode)
    }

    @Test
    fun `Google 응답에 ID Token이 없으면 인증 실패로 처리한다`() {
        server
            .expect(requestTo("https://oauth2.googleapis.com/token"))
            .andRespond(withSuccess("""{"access_token":"google-access-token"}""", MediaType.APPLICATION_JSON))

        val exception =
            assertFailsWith<BusinessException> {
                client.exchangeForIdToken(
                    "google-authorization-code",
                    "http://localhost:3000/oauth/google/callback",
                )
            }

        assertEquals(AuthErrorCode.INVALID_GOOGLE_AUTHORIZATION_CODE, exception.errorCode)
        server.verify()
    }

    companion object {
        private val properties =
            GoogleAuthProperties(
                clientId = "test-google-client-id",
                clientSecret = "test-google-client-secret",
                redirectUris =
                    setOf(
                        "http://localhost:3000/oauth/google/callback",
                        "https://belog.co.kr/oauth/google/callback",
                    ),
            )
    }
}
