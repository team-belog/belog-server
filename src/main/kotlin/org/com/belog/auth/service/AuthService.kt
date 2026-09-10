package org.com.belog.auth.service

import org.com.belog.auth.domain.GoogleLoginResult
import org.com.belog.auth.infrastructure.GoogleAuthorizationCodeClient
import org.com.belog.auth.infrastructure.GoogleIdTokenVerifier
import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val googleAuthorizationCodeClient: GoogleAuthorizationCodeClient,
    private val googleIdTokenVerifier: GoogleIdTokenVerifier,
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun loginWithGoogle(
        authorizationCode: String,
        redirectUri: String,
    ): GoogleLoginResult {
        val idToken = googleAuthorizationCodeClient.exchangeForIdToken(authorizationCode, redirectUri)
        val googleUserInfo = googleIdTokenVerifier.verify(idToken)
        val socialUser =
            userService.findOrCreateSocialUser(
                provider = SocialProvider.GOOGLE,
                providerUserId = googleUserInfo.providerUserId,
                email = googleUserInfo.email,
                nickname = googleUserInfo.name,
                profileImageUrl = googleUserInfo.profileImageUrl,
            )
        val tokens = jwtTokenProvider.createTokens(socialUser.userId)

        return GoogleLoginResult(
            tokens = tokens,
            isNewUser = socialUser.isNewUser,
        )
    }
}
