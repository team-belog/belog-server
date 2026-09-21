package org.com.belog.group.repository

import org.com.belog.group.domain.GroupMember
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun countByGroupId(groupId: Long): Long

    fun findByGroupIdAndUserId(
        groupId: Long,
        userId: Long,
    ): GroupMember?

    fun findAllByGroupIdAndIdIn(
        groupId: Long,
        memberIds: Collection<Long>,
    ): List<GroupMember>

    fun existsByGroupIdAndUserId(
        groupId: Long,
        userId: Long,
    ): Boolean

    @EntityGraph(attributePaths = ["user"])
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.group.id = :groupId
        ORDER BY member.role DESC, member.id ASC
        """,
    )
    fun findAllWithUserByGroupId(
        @Param("groupId") groupId: Long,
    ): List<GroupMember>
}
