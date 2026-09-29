package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface PostLogPhotoRepository : JpaRepository<PostLogPhoto, Long> {
    fun findAllByObjectKeyIn(objectKeys: Collection<String>): List<PostLogPhoto>
}
