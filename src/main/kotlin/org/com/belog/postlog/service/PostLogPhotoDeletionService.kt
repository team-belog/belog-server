package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.repository.PostLogPhotoLikeRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogTicketRepository
import org.com.belog.postlog.service.result.PostLogPhotoDeletionResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLogPhotoDeletionService(
    private val groupMemberFinder: GroupMemberFinder,
    private val photoRepository: PostLogPhotoRepository,
    private val photoLikeRepository: PostLogPhotoLikeRepository,
    private val ticketRepository: PostLogTicketRepository,
) {
    @Transactional
    fun delete(
        photoId: Long,
        userId: Long,
    ): PostLogPhotoDeletionResult {
        val photo =
            photoRepository.findByIdForUpdate(photoId)
                ?: throw BusinessException(PostLogErrorCode.POST_LOG_PHOTO_NOT_FOUND)
        val meeting = photo.meeting
        groupMemberFinder.findActiveMember(meeting.group, userId)

        photoLikeRepository.deleteAllByPhotoId(checkNotNull(photo.id) { "삭제 대상 사진의 ID가 없습니다." })
        photoRepository.delete(photo)

        return PostLogPhotoDeletionResult(
            objectKey = photo.objectKey,
            usedAsTicketCover =
                ticketRepository.existsBySourceMeetingIdAndCoverImageObjectKey(
                    sourceMeetingId = checkNotNull(meeting.id) { "삭제 대상 사진의 만남 ID가 없습니다." },
                    coverImageObjectKey = photo.objectKey,
                ),
        )
    }
}
