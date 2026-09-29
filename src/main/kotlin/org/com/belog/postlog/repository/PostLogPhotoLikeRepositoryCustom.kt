package org.com.belog.postlog.repository

interface PostLogPhotoLikeRepositoryCustom {
    fun saveIfAbsent(
        photoId: Long,
        groupMemberId: Long,
    )
}
