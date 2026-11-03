package org.com.belog.user.repository

import org.com.belog.user.domain.SocialProvider

interface UserRepositoryCustom {
    fun upsertSocialUser(
        email: String,
        provider: SocialProvider,
        providerUserId: String,
        socialProfileImageUrl: String?,
    ): SocialUserUpsertResult
}

data class SocialUserUpsertResult(
    val userId: Long,
    val onboardingRequired: Boolean,
    val socialProfileImageUrl: String?,
)
