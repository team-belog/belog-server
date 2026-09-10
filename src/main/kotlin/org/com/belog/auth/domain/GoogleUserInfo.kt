package org.com.belog.auth.domain

data class GoogleUserInfo(
    val providerUserId: String,
    val email: String,
    val name: String?,
    val profileImageUrl: String?,
)
