package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageUpload
import org.com.belog.group.infrastructure.S3GroupCoverImageUploadUrlProvider
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class GroupCoverImageService(
    private val userRepository: UserRepository,
    private val groupCoverImageUploadUrlProvider: S3GroupCoverImageUploadUrlProvider,
) {
    fun issueUploadUrl(
        userId: Long,
        contentType: String,
        fileSize: Long,
    ): GroupCoverImageUpload {
        val format =
            GroupCoverImageFormat.fromContentType(contentType)
                ?: throw BusinessException(GroupErrorCode.UNSUPPORTED_COVER_IMAGE_TYPE)

        if (fileSize <= 0 || fileSize > GroupCoverImageFormat.MAX_FILE_SIZE_BYTES) {
            throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_SIZE)
        }

        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        if (!user.isOnboardingCompleted) {
            throw BusinessException(GroupErrorCode.ONBOARDING_REQUIRED)
        }

        return groupCoverImageUploadUrlProvider.issueUploadUrl(userId, format, fileSize)
    }
}
