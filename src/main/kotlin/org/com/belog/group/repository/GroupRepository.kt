package org.com.belog.group.repository

import org.com.belog.group.domain.Group
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRepository : JpaRepository<Group, Long> {
    fun existsByInviteCode(inviteCode: String): Boolean
}
