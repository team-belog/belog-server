package org.com.belog.auth.service

import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleLoginResult
import org.com.belog.auth.infrastructure.GoogleAuthorizationCodeClient
import org.com.belog.auth.infrastructure.GoogleIdTokenVerifier
import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val googleAuthorizationCodeClient: GoogleAuthorizationCodeClient,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val loginTransactionService: LoginTransactionService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {
    fun loginWithGoogle(
        authorizationCode: String,
        redirectUri: String,
    ): GoogleLoginResult {
        val idToken = googleAuthorizationCodeClient.exchangeForIdToken(authorizationCode, redirectUri)
        val googleUserInfo = googleIdTokenVerifier.verify(idToken)

        return loginTransactionService.login(googleUserInfo)
    }

    @Transactional
    fun reissueTokens(refreshToken: String): AuthTokens {
        val userId = jwtTokenProvider.extractUserIdFromRefreshToken(refreshToken)
        val tokens = jwtTokenProvider.createTokens(userId)
        refreshTokenService.validateAndRotate(
            userId = userId,
            currentRefreshToken = refreshToken,
            newRefreshToken = tokens.refreshToken,
            expiration = tokens.refreshTokenExpiration,
        )

        return tokens
    }
}
