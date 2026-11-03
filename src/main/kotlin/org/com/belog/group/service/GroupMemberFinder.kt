package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.springframework.stereotype.Component

@Component
class GroupMemberFinder(
    private val groupMemberRepository: GroupMemberRepository,
) {
    fun findActiveMember(
        groupId: Long,
        userId: Long,
    ): GroupMember =
        groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)

    fun findActiveMember(
        group: Group,
        userId: Long,
    ): GroupMember = findActiveMember(checkNotNull(group.id) { "멤버 확인 대상 그룹의 ID가 없습니다." }, userId)
}
