package org.com.belog.auth.domain

import java.time.Duration

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiration: Duration,
    val refreshTokenExpiration: Duration,
)
