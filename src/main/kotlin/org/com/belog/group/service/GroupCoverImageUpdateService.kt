package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupCoverImageUpdateService(
    private val groupRepository: GroupRepository,
) {
    @Transactional
    fun update(
        groupId: Long,
        coverImageObjectKey: GroupCoverImageObjectKey,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
            }

        group.changeCoverImage(coverImageObjectKey)
    }
}
