package org.com.belog.user.service

import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.result.SocialUserResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun isNicknameAvailable(nickname: String): Boolean = !userRepository.existsByNickname(nickname)

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

        val upsertResult =
            userRepository.upsertSocialUser(
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                provider = provider,
                providerUserId = providerUserId,
            )

        return SocialUserResult(
            userId = upsertResult.userId,
            isNewUser = upsertResult.isNewUser,
        )
    }
}
