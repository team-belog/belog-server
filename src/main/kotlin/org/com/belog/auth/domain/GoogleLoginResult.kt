package org.com.belog.auth.domain

data class GoogleLoginResult(
    val tokens: AuthTokens,
    val isNewUser: Boolean,
)
