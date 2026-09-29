package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class PostLogPhotoRegistrationService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val photoRepository: PostLogPhotoRepository,
) {
    @Transactional
    fun register(
        meetingId: Long,
        userId: Long,
        targets: List<ValidatedPostLogPhotoRegistrationTarget>,
    ): List<PostLogPhoto> {
        val meeting =
            meetingRepository.findByIdWithGroupForUpdate(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val uploader =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val existingPhotosByObjectKey =
            photoRepository
                .findAllByObjectKeyIn(targets.map { target -> target.objectKey.value })
                .associateBy { photo -> photo.objectKey }

        targets.forEach { target ->
            val existingPhoto = existingPhotosByObjectKey[target.objectKey.value] ?: return@forEach
            if (!existingPhoto.matches(meetingId, target)) {
                throw BusinessException(PostLogErrorCode.PHOTO_OBJECT_KEY_CONFLICT)
            }
        }

        val newPhotos =
            targets
                .filterNot { target -> existingPhotosByObjectKey.containsKey(target.objectKey.value) }
                .map { target ->
                    PostLogPhoto.create(
                        meeting = meeting,
                        uploader = uploader,
                        objectKey = target.objectKey,
                        capturedAt = target.capturedAt,
                    )
                }
        val savedPhotosByObjectKey =
            photoRepository
                .saveAll(newPhotos)
                .associateBy { photo -> photo.objectKey }

        return targets.map { target ->
            existingPhotosByObjectKey[target.objectKey.value]
                ?: checkNotNull(savedPhotosByObjectKey[target.objectKey.value]) {
                    "등록된 Post-log 사진을 찾을 수 없습니다."
                }
        }
    }

    private fun PostLogPhoto.matches(
        meetingId: Long,
        target: ValidatedPostLogPhotoRegistrationTarget,
    ): Boolean =
        meeting.id == meetingId &&
            capturedAt == target.capturedAt.toInstant().truncatedTo(ChronoUnit.MICROS) &&
            capturedOffsetMinutes == target.capturedAt.offset.totalSeconds / PostLogPhoto.SECONDS_PER_MINUTE
}

data class ValidatedPostLogPhotoRegistrationTarget(
    val objectKey: PostLogPhotoObjectKey,
    val capturedAt: OffsetDateTime,
)
