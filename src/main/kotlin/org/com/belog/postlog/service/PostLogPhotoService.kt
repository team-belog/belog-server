package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoFormat
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.infrastructure.S3PostLogPhotoObjectVerifier
import org.com.belog.postlog.infrastructure.S3PostLogPhotoUploadUrlProvider
import org.com.belog.postlog.service.command.PostLogPhotoRegistrationTarget
import org.com.belog.postlog.service.command.PostLogPhotoUploadTarget
import org.com.belog.postlog.service.result.PostLogPhotoUploadResult
import org.springframework.stereotype.Service

@Service
class PostLogPhotoService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val uploadUrlProvider: S3PostLogPhotoUploadUrlProvider,
    private val objectVerifier: S3PostLogPhotoObjectVerifier,
    private val registrationService: PostLogPhotoRegistrationService,
) {
    fun issueUploadUrls(
        meetingId: Long,
        userId: Long,
        targets: List<PostLogPhotoUploadTarget>,
    ): List<PostLogPhotoUploadResult> {
        val uploadTargets = validateTargets(targets)
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

        return uploadTargets.map { target ->
            PostLogPhotoUploadResult(
                clientPhotoId = target.clientPhotoId,
                upload = uploadUrlProvider.issueUploadUrl(meetingId, target.format, target.fileSize),
            )
        }
    }

    fun registerPhotos(
        meetingId: Long,
        userId: Long,
        targets: List<PostLogPhotoRegistrationTarget>,
    ): List<PostLogPhoto> {
        val registrationTargets = validateRegistrationTargets(meetingId, targets)
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

        objectVerifier.verifyAll(registrationTargets.map { target -> target.objectKey })

        return registrationService.register(
            meetingId = meetingId,
            userId = userId,
            targets = registrationTargets,
        )
    }

    private fun validateTargets(targets: List<PostLogPhotoUploadTarget>): List<ValidatedUploadTarget> {
        if (targets.size !in 1..PostLogPhoto.MAX_UPLOAD_COUNT) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }
        if (targets.map { target -> target.clientPhotoId }.toSet().size != targets.size) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }

        return targets.map { target ->
            if (
                target.clientPhotoId.isBlank() ||
                target.clientPhotoId.length > PostLogPhotoUploadTarget.CLIENT_PHOTO_ID_MAX_LENGTH
            ) {
                throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
            }
            val format =
                PostLogPhotoFormat.fromContentType(target.contentType)
                    ?: throw BusinessException(PostLogErrorCode.UNSUPPORTED_PHOTO_TYPE)
            if (target.fileSize !in 1..PostLogPhotoFormat.MAX_FILE_SIZE_BYTES) {
                throw BusinessException(PostLogErrorCode.INVALID_PHOTO_SIZE)
            }

            ValidatedUploadTarget(
                clientPhotoId = target.clientPhotoId,
                format = format,
                fileSize = target.fileSize,
            )
        }
    }

    private fun validateRegistrationTargets(
        meetingId: Long,
        targets: List<PostLogPhotoRegistrationTarget>,
    ): List<ValidatedPostLogPhotoRegistrationTarget> {
        if (targets.size !in 1..PostLogPhoto.MAX_UPLOAD_COUNT) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }
        if (targets.map { target -> target.objectKey }.toSet().size != targets.size) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }

        return targets.map { target ->
            val objectKey =
                try {
                    PostLogPhotoObjectKey.create(meetingId, target.objectKey)
                } catch (exception: IllegalArgumentException) {
                    throw BusinessException(PostLogErrorCode.INVALID_PHOTO_OBJECT_KEY, exception)
                }
            if (target.capturedAt.offset.totalSeconds % PostLogPhoto.SECONDS_PER_MINUTE != 0) {
                throw BusinessException(PostLogErrorCode.INVALID_CAPTURED_AT)
            }

            ValidatedPostLogPhotoRegistrationTarget(
                objectKey = objectKey,
                capturedAt = target.capturedAt,
            )
        }
    }

    private data class ValidatedUploadTarget(
        val clientPhotoId: String,
        val format: PostLogPhotoFormat,
        val fileSize: Long,
    )
}
