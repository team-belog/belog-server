package org.com.belog.auth.service

import org.com.belog.auth.domain.GoogleLoginResult
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginTransactionService(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
) {
    @Transactional
    fun login(googleUserInfo: GoogleUserInfo): GoogleLoginResult {
        val socialUser =
            userService.findOrCreateSocialUser(
                provider = SocialProvider.GOOGLE,
                providerUserId = googleUserInfo.providerUserId,
                email = googleUserInfo.email,
            )
        val tokens = jwtTokenProvider.createTokens(socialUser.userId)
        refreshTokenService.saveOrUpdate(
            userId = socialUser.userId,
            refreshToken = tokens.refreshToken,
            expiration = tokens.refreshTokenExpiration,
        )

        return GoogleLoginResult(
            tokens = tokens,
            onboardingRequired = socialUser.onboardingRequired,
        )
    }
}
