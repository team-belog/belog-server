package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.result.UserProfileResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val profileImageService: ProfileImageService,
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
}
