package org.com.belog.auth.infrastructure

import org.com.belog.auth.config.GoogleAuthProperties
import org.com.belog.auth.config.GoogleTokenConfig
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.response.code.AuthErrorCode
import org.com.belog.global.error.BusinessException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoogleIdTokenVerifier(
    @Qualifier(GoogleTokenConfig.GOOGLE_JWT_DECODER)
    private val jwtDecoder: JwtDecoder,
    private val properties: GoogleAuthProperties,
) {
    fun verify(idToken: String): GoogleUserInfo {
        if (idToken.isBlank()) {
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN)
        }

        val jwt = decode(idToken)
        validateClaims(jwt)

        return GoogleUserInfo(
            providerUserId = requiredClaim(jwt.subject),
            email = requiredClaim(jwt.getClaimAsString(EMAIL_CLAIM)),
            name = jwt.getClaimAsString(NAME_CLAIM),
            profileImageUrl = jwt.getClaimAsString(PICTURE_CLAIM),
        )
    }

    private fun decode(idToken: String): Jwt =
        try {
            jwtDecoder.decode(idToken)
        } catch (exception: JwtException) {
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN, exception)
        }

    private fun validateClaims(jwt: Jwt) {
        val issuer = jwt.issuer?.toString()
        val expiresAt = jwt.expiresAt
        val valid =
            issuer in GOOGLE_ISSUERS &&
                jwt.audience?.contains(properties.clientId) == true &&
                expiresAt != null &&
                expiresAt.isAfter(Instant.now()) &&
                jwt.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM) == true &&
                !jwt.subject.isNullOrBlank() &&
                !jwt.getClaimAsString(EMAIL_CLAIM).isNullOrBlank()

        if (!valid) {
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN)
        }
    }

    private fun requiredClaim(value: String?): String = value ?: throw BusinessException(AuthErrorCode.INVALID_GOOGLE_ID_TOKEN)

    companion object {
        private val GOOGLE_ISSUERS = setOf("accounts.google.com", "https://accounts.google.com")
        private const val EMAIL_CLAIM = "email"
        private const val EMAIL_VERIFIED_CLAIM = "email_verified"
        private const val NAME_CLAIM = "name"
        private const val PICTURE_CLAIM = "picture"
    }
}
