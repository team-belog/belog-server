package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.USER_NICKNAME_UNIQUE_CONSTRAINT_NAME
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.command.ProfileImageChange
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileUpdateTransactionService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateProfile(
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

        updateNickname(user, nickname)
        updateProfileImage(user, profileImageChange)
        flushProfile(user)
    }

    private fun updateNickname(
        user: User,
        nickname: String?,
    ) {
        if (nickname == null) {
            return
        }

        val normalizedNickname = nickname.trim()
        if (normalizedNickname != user.nickname && userRepository.existsByNickname(normalizedNickname)) {
            throw BusinessException(UserErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        user.updateNickname(normalizedNickname)
    }

    private fun updateProfileImage(
        user: User,
        profileImageChange: ProfileImageChange?,
    ) {
        when (profileImageChange) {
            is ProfileImageChange.Update -> user.updateProfileImage(profileImageChange.objectKey)
            ProfileImageChange.Reset -> user.resetProfileImage()
            null -> Unit
        }
    }

    private fun flushProfile(user: User) {
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
