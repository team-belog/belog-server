package org.com.belog.auth.domain

data class GoogleUserInfo(
    val providerUserId: String,
    val email: String,
    val profileImageUrl: String? = null,
)
