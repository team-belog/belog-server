package org.com.belog.group.repository

import org.com.belog.group.domain.GroupMember
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun countByGroupId(groupId: Long): Long

    fun existsByGroupIdAndUserId(
        groupId: Long,
        userId: Long,
    ): Boolean
}
