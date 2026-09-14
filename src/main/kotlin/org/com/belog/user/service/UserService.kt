package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.USER_NICKNAME_UNIQUE_CONSTRAINT_NAME
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun isNicknameAvailable(nickname: String): Boolean = !userRepository.existsByNickname(nickname)

    @Transactional
    fun completeOnboarding(
        userId: Long,
        profileImageObjectKey: ProfileImageObjectKey,
        nickname: String,
        bankAccount: BankAccount,
    ) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        if (user.isOnboardingCompleted) {
            throw BusinessException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED)
        }

        val normalizedNickname = nickname.trim()
        if (userRepository.existsByNickname(normalizedNickname)) {
            throw BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        user.completeOnboarding(
            profileImageObjectKey = profileImageObjectKey,
            nickname = normalizedNickname,
            bankAccount = bankAccount,
            completedAt = Instant.now(clock),
        )

        flushOnboarding(user)
    }

    @Transactional
    fun findOrCreateSocialUser(
        provider: SocialProvider,
        providerUserId: String,
        email: String,
    ): SocialUserResult {
        val existingUser =
            userRepository.findByProviderAndProviderUserId(
                provider = provider,
                providerUserId = providerUserId,
            )

        if (existingUser != null) {
            return SocialUserResult(
                userId = requireNotNull(existingUser.id),
                onboardingRequired = !existingUser.isOnboardingCompleted,
            )
        }

        val upsertResult =
            userRepository.upsertSocialUser(
                email = email,
                provider = provider,
                providerUserId = providerUserId,
            )

        return SocialUserResult(
            userId = upsertResult.userId,
            onboardingRequired = upsertResult.onboardingRequired,
        )
    }

    private fun flushOnboarding(user: User) {
        try {
            userRepository.saveAndFlush(user)
        } catch (exception: DataIntegrityViolationException) {
            if (exception.isNicknameUniqueConstraintViolation()) {
                throw BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS, exception)
            }
            throw exception
        }
    }

    private fun DataIntegrityViolationException.isNicknameUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(USER_NICKNAME_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }
}
