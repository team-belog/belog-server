package org.com.belog.group.repository

import jakarta.persistence.LockModeType
import org.com.belog.group.domain.GroupMember
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun countByGroupId(groupId: Long): Long

    fun findByGroupIdAndUserId(
        groupId: Long,
        userId: Long,
    ): GroupMember?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.group.id = :groupId
          AND member.user.id = :userId
        """,
    )
    fun findByGroupIdAndUserIdForUpdate(
        @Param("groupId") groupId: Long,
        @Param("userId") userId: Long,
    ): GroupMember?

    fun findAllByGroupIdAndIdIn(
        groupId: Long,
        memberIds: Collection<Long>,
    ): List<GroupMember>

    fun existsByGroupIdAndUserId(
        groupId: Long,
        userId: Long,
    ): Boolean

    @EntityGraph(attributePaths = ["group"])
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.user.id = :userId
          AND (
              :cursorId IS NULL
              OR (:cursorPinned = TRUE AND member.pinned = FALSE)
              OR (member.pinned = :cursorPinned AND member.id < :cursorId)
          )
        ORDER BY member.pinned DESC, member.id DESC
        """,
    )
    fun findMyGroupPage(
        @Param("userId") userId: Long,
        @Param("cursorPinned") cursorPinned: Boolean?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable,
    ): List<GroupMember>

    @EntityGraph(attributePaths = ["group", "user"])
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.group.id IN :groupIds
        ORDER BY member.group.id ASC, member.role DESC, member.id ASC
        """,
    )
    fun findAllWithUserByGroupIdIn(
        @Param("groupIds") groupIds: Collection<Long>,
    ): List<GroupMember>

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

    @EntityGraph(attributePaths = ["user"])
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.group.id = :groupId
          AND (
              LOCATE(LOWER(:query), LOWER(member.user.nickname)) > 0
              OR LOCATE(LOWER(:query), LOWER(member.user.name)) > 0
          )
        ORDER BY member.role DESC, member.id ASC
        """,
    )
    fun searchAllWithUserByGroupId(
        @Param("groupId") groupId: Long,
        @Param("query") query: String,
    ): List<GroupMember>
}
