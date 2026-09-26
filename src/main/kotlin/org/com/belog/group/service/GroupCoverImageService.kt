package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.GroupCoverImageUpload
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.infrastructure.S3GroupCoverImageObjectVerifier
import org.com.belog.group.infrastructure.S3GroupCoverImageUploadUrlProvider
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class GroupCoverImageService(
    private val userRepository: UserRepository,
    private val groupCoverImageUploadUrlProvider: S3GroupCoverImageUploadUrlProvider,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupCoverImageObjectVerifier: S3GroupCoverImageObjectVerifier,
    private val groupCoverImageUpdateService: GroupCoverImageUpdateService,
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

    fun updateCoverImage(
        groupId: Long,
        userId: Long,
        coverImageObjectKey: GroupCoverImageObjectKey,
    ) {
        if (!groupRepository.existsById(groupId)) {
            throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        }

        val currentMember =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)

        if (currentMember.role != GroupRole.OWNER) {
            throw BusinessException(GroupErrorCode.GROUP_OWNER_REQUIRED)
        }

        groupCoverImageObjectVerifier.verify(coverImageObjectKey)
        groupCoverImageUpdateService.update(groupId, coverImageObjectKey)
    }
}
