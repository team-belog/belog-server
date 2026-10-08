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
    fun countByGroupIdAndWithdrawnAtIsNull(groupId: Long): Long

    fun findByGroupIdAndUserIdAndWithdrawnAtIsNull(
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

    fun findAllByGroupIdAndIdInAndWithdrawnAtIsNull(
        groupId: Long,
        memberIds: Collection<Long>,
    ): List<GroupMember>

    fun existsByGroupIdAndUserIdAndWithdrawnAtIsNull(
        groupId: Long,
        userId: Long,
    ): Boolean

    fun findAllByUserIdAndWithdrawnAtIsNull(userId: Long): List<GroupMember>

    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.group.id = :groupId
          AND member.id <> :excludedMemberId
          AND member.withdrawnAt IS NULL
        ORDER BY member.createdAt ASC, member.id ASC
        """,
    )
    fun findEarliestActiveMember(
        @Param("groupId") groupId: Long,
        @Param("excludedMemberId") excludedMemberId: Long,
        pageable: Pageable,
    ): List<GroupMember>

    @EntityGraph(attributePaths = ["group"])
    @Query(
        """
        SELECT member
        FROM GroupMember member
        WHERE member.user.id = :userId
          AND member.withdrawnAt IS NULL
          AND member.group.deletedAt IS NULL
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
          AND member.withdrawnAt IS NULL
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
          AND member.withdrawnAt IS NULL
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
          AND member.withdrawnAt IS NULL
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
