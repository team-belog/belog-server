package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.ProfileImageSource
import org.com.belog.user.domain.ProfileImageUpload
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.USER_NICKNAME_UNIQUE_CONSTRAINT_NAME
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.ProfileImageStorage
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.command.ProfileImageChange
import org.com.belog.user.service.result.SocialUserResult
import org.com.belog.user.service.result.UserProfileResult
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val profileImageStorage: ProfileImageStorage,
    transactionManager: PlatformTransactionManager,
    private val clock: Clock,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Transactional(readOnly = true)
    fun isNicknameAvailable(nickname: String): Boolean = !userRepository.existsActiveNickname(nickname)

    @Transactional(readOnly = true)
    fun getBankAccount(userId: Long): BankAccount {
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        return user.bankAccount
            ?: throw BusinessException(UserErrorCode.BANK_ACCOUNT_NOT_REGISTERED)
    }

    @Transactional
    fun updateBankAccount(
        userId: Long,
        bankAccount: BankAccount,
    ) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        if (user.bankAccount == null) {
            throw BusinessException(UserErrorCode.BANK_ACCOUNT_NOT_REGISTERED)
        }

        user.updateBankAccount(bankAccount)
    }

    @Transactional(readOnly = true)
    fun getProfile(userId: Long): UserProfileResult {
        val user = findOnboardedUser(userId)

        return UserProfileResult(
            nickname = requireNotNull(user.nickname),
            email = user.email,
            profileImageUrl = resolveProfileImageUrl(user),
        )
    }

    fun issueProfileImageUploadUrl(
        userId: Long,
        contentType: String,
        fileSize: Long,
    ): ProfileImageUpload {
        val format =
            ProfileImageFormat.fromContentType(contentType)
                ?: throw BusinessException(UserErrorCode.UNSUPPORTED_PROFILE_IMAGE_TYPE)

        if (fileSize <= 0 || fileSize > ProfileImageFormat.MAX_FILE_SIZE_BYTES) {
            throw BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE_SIZE)
        }

        if (!userRepository.existsById(userId)) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }

        return profileImageStorage.issueUploadUrl(userId, format, fileSize)
    }

    fun resolveProfileImageUrl(user: User): String? {
        if (!user.isActive) {
            return null
        }

        return when (user.profileImageSource) {
            ProfileImageSource.SOCIAL -> user.socialProfileImageUrl
            ProfileImageSource.CUSTOM -> {
                val objectKey =
                    checkNotNull(user.profileImageObjectKey) {
                        "직접 업로드한 프로필 이미지의 object key가 없습니다."
                    }
                profileImageStorage.generateReadUrl(objectKey)
            }
            ProfileImageSource.DEFAULT -> null
        }
    }

    fun resolveDisplayNickname(user: User): String =
        if (user.isActive) {
            requireNotNull(user.nickname)
        } else {
            WITHDRAWN_USER_DISPLAY_NICKNAME
        }

    fun completeOnboarding(
        userId: Long,
        profileImageObjectKey: ProfileImageObjectKey?,
        nickname: String,
        name: String,
        bankAccount: BankAccount,
    ) {
        profileImageObjectKey?.let(profileImageStorage::verify)

        transactionTemplate.executeWithoutResult {
            completeOnboardingInTransaction(
                userId = userId,
                profileImageObjectKey = profileImageObjectKey,
                nickname = nickname,
                name = name,
                bankAccount = bankAccount,
            )
        }
    }

    fun updateProfile(
        userId: Long,
        nickname: String?,
        profileImageChange: ProfileImageChange?,
    ) {
        require(nickname != null || profileImageChange != null) {
            "수정할 프로필 정보가 필요합니다."
        }

        if (profileImageChange is ProfileImageChange.Update) {
            profileImageStorage.verify(profileImageChange.objectKey)
        }

        transactionTemplate.executeWithoutResult {
            updateProfileInTransaction(
                userId = userId,
                nickname = nickname,
                profileImageChange = profileImageChange,
            )
        }
    }

    private fun completeOnboardingInTransaction(
        userId: Long,
        profileImageObjectKey: ProfileImageObjectKey?,
        nickname: String,
        name: String,
        bankAccount: BankAccount,
    ) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        if (user.isOnboardingCompleted) {
            throw BusinessException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED)
        }

        val normalizedNickname = nickname.trim()
        if (userRepository.existsActiveNickname(normalizedNickname)) {
            throw BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        user.completeOnboarding(
            profileImageObjectKey = profileImageObjectKey,
            nickname = normalizedNickname,
            name = name,
            bankAccount = bankAccount,
            completedAt = Instant.now(clock),
        )

        flushUser(user)
    }

    @Transactional
    fun findOrCreateSocialUser(
        provider: SocialProvider,
        providerUserId: String,
        email: String,
        socialProfileImageUrl: String? = null,
    ): SocialUserResult {
        val activeUser = findActiveSocialUserForUpdate(provider, providerUserId)

        if (activeUser != null) {
            activeUser.updateSocialProfileImageUrl(socialProfileImageUrl)
            return SocialUserResult(
                userId = requireNotNull(activeUser.id),
                onboardingRequired = !activeUser.isOnboardingCompleted,
                socialProfileImageUrl = activeUser.socialProfileImageUrl,
            )
        }

        val upsertResult =
            userRepository.upsertSocialUser(
                email = email,
                provider = provider,
                providerUserId = providerUserId,
                socialProfileImageUrl = socialProfileImageUrl,
            )

        return SocialUserResult(
            userId = upsertResult.userId,
            onboardingRequired = upsertResult.onboardingRequired,
            socialProfileImageUrl = upsertResult.socialProfileImageUrl,
        )
    }

    private fun findActiveSocialUserForUpdate(
        provider: SocialProvider,
        providerUserId: String,
    ): User? {
        val userId =
            userRepository.findActiveSocialUserId(
                provider = provider,
                providerUserId = providerUserId,
            ) ?: return null

        return userRepository.findByIdForUpdate(userId)?.takeIf(User::isActive)
    }

    private fun updateProfileInTransaction(
        userId: Long,
        nickname: String?,
        profileImageChange: ProfileImageChange?,
    ) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        if (!user.isOnboardingCompleted) {
            throw BusinessException(UserErrorCode.ONBOARDING_REQUIRED)
        }

        if (nickname != null) {
            val normalizedNickname = nickname.trim()
            if (
                normalizedNickname != user.nickname &&
                userRepository.existsActiveNickname(normalizedNickname)
            ) {
                throw BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS)
            }
            user.updateNickname(normalizedNickname)
        }

        when (profileImageChange) {
            is ProfileImageChange.Update -> user.updateProfileImage(profileImageChange.objectKey)
            ProfileImageChange.Reset -> user.resetProfileImage()
            null -> Unit
        }

        flushUser(user)
    }

    private fun findOnboardedUser(userId: Long): User {
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        if (!user.isOnboardingCompleted) {
            throw BusinessException(UserErrorCode.ONBOARDING_REQUIRED)
        }

        return user
    }

    private fun flushUser(user: User) {
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

    companion object {
        private const val WITHDRAWN_USER_DISPLAY_NICKNAME = "탈퇴한 사용자"
    }
}
