package org.com.belog.postlog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.repository.PostLogPhotoLikeRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.service.result.PostLogPhotoLikeResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLogPhotoLikeService(
    private val groupMemberRepository: GroupMemberRepository,
    private val photoRepository: PostLogPhotoRepository,
    private val photoLikeRepository: PostLogPhotoLikeRepository,
) {
    @Transactional
    fun likePhoto(
        photoId: Long,
        userId: Long,
    ): PostLogPhotoLikeResult {
        val target = findLikeTarget(photoId, userId)

        photoLikeRepository.saveIfAbsent(
            photoId = target.photoId,
            groupMemberId = target.groupMemberId,
        )

        return createResult(
            photoId = target.photoId,
            likedByMe = true,
        )
    }

    @Transactional
    fun unlikePhoto(
        photoId: Long,
        userId: Long,
    ): PostLogPhotoLikeResult {
        val target = findLikeTarget(photoId, userId)

        photoLikeRepository.deleteByPhotoIdAndGroupMemberId(
            photoId = target.photoId,
            groupMemberId = target.groupMemberId,
        )

        return createResult(
            photoId = target.photoId,
            likedByMe = false,
        )
    }

    private fun findLikeTarget(
        photoId: Long,
        userId: Long,
    ): PhotoLikeTarget {
        val photo = findPhoto(photoId)
        val meeting = photo.meeting
        val groupMemberId = findGroupMemberId(meeting, userId)

        return PhotoLikeTarget(
            photoId = checkNotNull(photo.id) { "좋아요 대상 사진의 ID가 없습니다." },
            groupMemberId = groupMemberId,
        )
    }

    private fun findGroupMemberId(
        meeting: Meeting,
        userId: Long,
    ): Long {
        val groupId = checkNotNull(meeting.group.id) { "좋아요 대상 만남의 그룹 ID가 없습니다." }
        val groupMember =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)

        return checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." }
    }

    private fun findPhoto(photoId: Long): PostLogPhoto =
        photoRepository.findByIdWithMeeting(photoId)
            ?: throw BusinessException(PostLogErrorCode.POST_LOG_PHOTO_NOT_FOUND)

    private fun createResult(
        photoId: Long,
        likedByMe: Boolean,
    ): PostLogPhotoLikeResult =
        PostLogPhotoLikeResult(
            photoId = photoId,
            likedByMe = likedByMe,
            likeCount = photoLikeRepository.countByPhotoId(photoId),
        )

    private data class PhotoLikeTarget(
        val photoId: Long,
        val groupMemberId: Long,
    )
}
