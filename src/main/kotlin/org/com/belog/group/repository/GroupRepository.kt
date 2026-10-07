package org.com.belog.group.repository

import jakarta.persistence.LockModeType
import org.com.belog.group.domain.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupRepository : JpaRepository<Group, Long> {
    fun existsByInviteCode(inviteCode: String): Boolean

    fun existsByIdAndDeletedAtIsNull(id: Long): Boolean

    @Query(
        """
        SELECT group
        FROM Group group
        WHERE group.id = :groupId
          AND group.deletedAt IS NULL
        """,
    )
    fun findActiveById(
        @Param("groupId") groupId: Long,
    ): Group?

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
        """
        SELECT group
        FROM Group group
        WHERE group.id = :groupId
          AND group.deletedAt IS NULL
        """,
    )
    fun findActiveByIdForShare(
        @Param("groupId") groupId: Long,
    ): Group?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT group
        FROM Group group
        WHERE group.inviteCode = :inviteCode
          AND group.deletedAt IS NULL
        """,
    )
    fun findByInviteCodeForUpdate(
        @Param("inviteCode") inviteCode: String,
    ): Group?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT group
        FROM Group group
        WHERE group.id = :groupId
          AND group.deletedAt IS NULL
        """,
    )
    fun findActiveByIdForUpdate(
        @Param("groupId") groupId: Long,
    ): Group?
}
