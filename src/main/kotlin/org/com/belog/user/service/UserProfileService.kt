package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.infrastructure.S3ProfileImageObjectVerifier
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.command.ProfileImageChange
import org.com.belog.user.service.result.UserProfileResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val profileImageService: ProfileImageService,
    private val profileImageObjectVerifier: S3ProfileImageObjectVerifier,
    private val userProfileUpdateTransactionService: UserProfileUpdateTransactionService,
) {
    @Transactional(readOnly = true)
    fun getProfile(userId: Long): UserProfileResult {
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        if (!user.isOnboardingCompleted) {
            throw BusinessException(UserErrorCode.ONBOARDING_REQUIRED)
        }

        return UserProfileResult(
            nickname = requireNotNull(user.nickname),
            profileImageUrl = profileImageService.resolveProfileImageUrl(user),
        )
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
            profileImageObjectVerifier.verify(profileImageChange.objectKey)
        }

        userProfileUpdateTransactionService.updateProfile(
            userId = userId,
            nickname = nickname,
            profileImageChange = profileImageChange,
        )
    }
}
