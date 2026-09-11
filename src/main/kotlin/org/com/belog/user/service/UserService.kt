package org.com.belog.user.service

import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun findOrCreateSocialUser(
        provider: SocialProvider,
        providerUserId: String,
        email: String,
        nickname: String?,
        profileImageUrl: String?,
    ): SocialUserResult {
        val existingUser =
            userRepository.findByProviderAndProviderUserId(
                provider = provider,
                providerUserId = providerUserId,
            )

        if (existingUser != null) {
            return SocialUserResult(
                userId = requireNotNull(existingUser.id),
                isNewUser = false,
            )
        }

        userRepository.upsertSocialUser(
            email = email,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            provider = provider.name,
            providerUserId = providerUserId,
        )
        val user =
            checkNotNull(
                userRepository.findByProviderAndProviderUserIdForUpdate(
                    provider = provider,
                    providerUserId = providerUserId,
                ),
            ) {
                "Upsert된 소셜 사용자를 조회할 수 없습니다."
            }

        return SocialUserResult(
            userId = requireNotNull(user.id),
            isNewUser = true,
        )
    }
}
