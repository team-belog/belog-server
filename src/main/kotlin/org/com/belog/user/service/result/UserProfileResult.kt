package org.com.belog.user.service.result

data class UserProfileResult(
    val nickname: String,
    val email: String,
    val profileImageUrl: String?,
)
