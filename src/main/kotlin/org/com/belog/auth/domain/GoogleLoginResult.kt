package org.com.belog.auth.domain

data class GoogleLoginResult(
    val tokens: AuthTokens,
    val onboardingRequired: Boolean,
    val socialProfileImageUrl: String? = null,
)
