package org.com.belog.auth.domain

data class TokenReissueResult(
    val tokens: AuthTokens,
    val onboardingRequired: Boolean,
)
