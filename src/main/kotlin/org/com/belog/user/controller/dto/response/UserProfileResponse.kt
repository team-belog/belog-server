package org.com.belog.user.controller.dto.response

import org.com.belog.user.service.result.UserProfileResult

data class UserProfileResponse(
    val nickname: String,
    val email: String,
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: UserProfileResult): UserProfileResponse =
            UserProfileResponse(
                nickname = result.nickname,
                email = result.email,
                profileImageUrl = result.profileImageUrl,
            )
    }
}
