package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
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
import org.com.belog.postlog.repository.PostLogPhotoLikeRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.service.command.PostLogPhotoRegistrationTarget
import org.com.belog.postlog.service.command.PostLogPhotoUploadTarget
import org.com.belog.postlog.service.result.PostLogPhotoListItemResult
import org.com.belog.postlog.service.result.PostLogPhotoListResult
import org.com.belog.postlog.service.result.PostLogPhotoUploadResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

@Service
class PostLogPhotoService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val uploadUrlProvider: S3PostLogPhotoUploadUrlProvider,
    private val objectVerifier: S3PostLogPhotoObjectVerifier,
    private val registrationService: PostLogPhotoRegistrationService,
    private val photoRepository: PostLogPhotoRepository,
    private val photoLikeRepository: PostLogPhotoLikeRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
) {
    @Transactional(readOnly = true)
    fun getPhotos(
        meetingId: Long,
        userId: Long,
        cursor: String?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): PostLogPhotoListResult {
        require(size in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            "사진 조회 개수는 ${MIN_PAGE_SIZE}개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
        }

        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val groupMember =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val groupMemberId = checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." }
        val decodedCursor = cursor?.let(::decodeCursor)
        val pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT)
        val photos =
            if (decodedCursor == null) {
                photoRepository.findPage(meetingId = meetingId, pageable = pageable)
            } else {
                photoRepository.findPageAfter(
                    meetingId = meetingId,
                    capturedAt = decodedCursor.capturedAt,
                    photoId = decodedCursor.photoId,
                    pageable = pageable,
                )
            }
        val hasNext = photos.size > size
        val pagePhotos = photos.take(size)
        val photoIds = pagePhotos.map { photo -> checkNotNull(photo.id) { "조회된 사진의 ID가 없습니다." } }
        val likeCounts =
            if (photoIds.isEmpty()) {
                emptyMap()
            } else {
                photoLikeRepository
                    .countByPhotoIds(photoIds)
                    .associate { count -> count.photoId to count.likeCount }
            }
        val likedPhotoIds =
            if (photoIds.isEmpty()) {
                emptySet()
            } else {
                photoLikeRepository.findLikedPhotoIds(photoIds, groupMemberId).toSet()
            }

        return PostLogPhotoListResult(
            items =
                pagePhotos.mapIndexed { index, photo ->
                    val photoId = photoIds[index]
                    PostLogPhotoListItemResult(
                        photoId = photoId,
                        photoUrl = objectReadUrlProvider.generateReadUrl(photo.objectKey),
                        capturedAt = photo.capturedAtWithOffset(),
                        likeCount = likeCounts[photoId] ?: 0L,
                        likedByMe = photoId in likedPhotoIds,
                    )
                },
            nextCursor = pagePhotos.lastOrNull()?.takeIf { hasNext }?.let(::encodeCursor),
            hasNext = hasNext,
        )
    }

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

        if (!groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)) {
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

        if (!groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)) {
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

    private fun encodeCursor(photo: PostLogPhoto): String {
        val photoId = checkNotNull(photo.id) { "커서 대상 사진의 ID가 없습니다." }
        val value = "${photo.capturedAt}$CURSOR_DELIMITER$photoId"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeCursor(cursor: String): PhotoCursor =
        try {
            val decoded = String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            val parts = decoded.split(CURSOR_DELIMITER, limit = CURSOR_PART_COUNT)
            require(parts.size == CURSOR_PART_COUNT)
            val photoId = parts[1].toLong()
            require(photoId > 0)

            PhotoCursor(
                capturedAt = Instant.parse(parts[0]),
                photoId = photoId,
            )
        } catch (exception: RuntimeException) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_CURSOR, exception)
        }

    private data class PhotoCursor(
        val capturedAt: Instant,
        val photoId: Long,
    )

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val MAX_PAGE_SIZE = 50
        private const val MIN_PAGE_SIZE = 1
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
        private const val CURSOR_DELIMITER = "|"
        private const val CURSOR_PART_COUNT = 2
    }
}
