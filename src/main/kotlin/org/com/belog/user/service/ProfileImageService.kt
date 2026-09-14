package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageUpload
import org.com.belog.user.infrastructure.S3ProfileImageUploadUrlProvider
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class ProfileImageService(
    private val userRepository: UserRepository,
    private val profileImageUploadUrlProvider: S3ProfileImageUploadUrlProvider,
) {
    fun issueUploadUrl(
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

        return profileImageUploadUrlProvider.issueUploadUrl(userId, format)
    }
}
