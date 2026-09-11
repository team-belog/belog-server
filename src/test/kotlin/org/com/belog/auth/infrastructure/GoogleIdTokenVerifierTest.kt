package org.com.belog.auth.infrastructure

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.config.GoogleAuthProperties
import org.com.belog.global.error.BusinessException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoogleIdTokenVerifierTest {
    @Test
    fun `유효한 Google ID 토큰에서 사용자 정보를 반환한다`() {
        val verifier = verifierReturning(validJwt())

        val userInfo = verifier.verify("google-id-token")

        assertEquals("google-subject", userInfo.providerUserId)
        assertEquals("user@example.com", userInfo.email)
        assertEquals("belog", userInfo.name)
        assertEquals("https://example.com/profile.png", userInfo.profileImageUrl)
    }

    @Test
    fun `대상 Client ID가 다르면 인증에 실패한다`() {
        val jwt = validJwt(audience = listOf("another-client-id"))

        assertInvalidToken(verifierReturning(jwt))
    }

    @Test
    fun `다중 audience의 azp가 지원하는 Client ID이면 인증에 성공한다`() {
        val jwt =
            validJwt(
                audience = listOf(CLIENT_ID, "another-client-id"),
                authorizedParty = CLIENT_ID,
            )
        val verifier = verifierReturning(jwt)

        val userInfo = verifier.verify("google-id-token")

        assertEquals("google-subject", userInfo.providerUserId)
    }

    @Test
    fun `다중 audience에 azp가 없으면 인증에 실패한다`() {
        val jwt = validJwt(audience = listOf(CLIENT_ID, "another-client-id"))

        assertInvalidToken(verifierReturning(jwt))
    }

    @Test
    fun `다중 audience의 azp가 지원하지 않는 Client ID이면 인증에 실패한다`() {
        val jwt =
            validJwt(
                audience = listOf(CLIENT_ID, "another-client-id"),
                authorizedParty = "another-client-id",
            )

        assertInvalidToken(verifierReturning(jwt))
    }

    @Test
    fun `이메일이 검증되지 않았으면 인증에 실패한다`() {
        val jwt = validJwt(emailVerified = false)

        assertInvalidToken(verifierReturning(jwt))
    }

    @Test
    fun `토큰 서명 검증에 실패하면 인증에 실패한다`() {
        val decoder = JwtDecoder { throw JwtException("invalid signature") }
        val verifier = GoogleIdTokenVerifier(decoder, properties)

        assertInvalidToken(verifier)
    }

    private fun assertInvalidToken(verifier: GoogleIdTokenVerifier) {
        val exception = assertFailsWith<BusinessException> { verifier.verify("google-id-token") }
        assertEquals(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN, exception.errorCode)
    }

    private fun verifierReturning(jwt: Jwt): GoogleIdTokenVerifier =
        GoogleIdTokenVerifier(
            jwtDecoder = JwtDecoder { jwt },
            properties = properties,
        )

    private fun validJwt(
        audience: List<String> = listOf(CLIENT_ID),
        authorizedParty: String? = null,
        emailVerified: Boolean = true,
    ): Jwt {
        val builder =
            Jwt
                .withTokenValue("google-id-token")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject("google-subject")
                .audience(audience)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "user@example.com")
                .claim("email_verified", emailVerified)
                .claim("name", "belog")
                .claim("picture", "https://example.com/profile.png")

        if (authorizedParty != null) {
            builder.claim("azp", authorizedParty)
        }

        return builder.build()
    }

    companion object {
        private const val CLIENT_ID = "test-google-client-id"
        private val properties =
            GoogleAuthProperties(
                clientId = CLIENT_ID,
                clientSecret = "test-google-client-secret",
                redirectUris = setOf("http://localhost:3000/oauth/google/callback"),
            )
    }
}
