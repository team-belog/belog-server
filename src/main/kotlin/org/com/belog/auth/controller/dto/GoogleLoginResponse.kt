package org.com.belog.auth.controller.dto

data class GoogleLoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val onboardingRequired: Boolean,
)
