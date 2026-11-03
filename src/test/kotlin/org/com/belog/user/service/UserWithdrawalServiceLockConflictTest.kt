package org.com.belog.user.service

import org.com.belog.auth.service.RefreshTokenService
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.service.GroupService
import org.com.belog.meeting.service.MeetingService
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.CannotAcquireLockException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserWithdrawalServiceLockConflictTest {
    private val userRepository = mock(UserRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val groupService = mock(GroupService::class.java)
    private val meetingService = mock(MeetingService::class.java)
    private val settlementRequestService = mock(SettlementRequestService::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC)
    private val userWithdrawalService =
        UserWithdrawalService(
            userRepository = userRepository,
            groupMemberRepository = groupMemberRepository,
            groupService = groupService,
            meetingService = meetingService,
            settlementRequestService = settlementRequestService,
            refreshTokenService = refreshTokenService,
            clock = clock,
        )

    @Test
    fun `그룹 방장 위임 처리 중 락 경쟁이 발생하면 탈퇴 처리 충돌로 변환한다`() {
        val activeUser = mock(User::class.java)
        `when`(activeUser.isActive).thenReturn(true)
        `when`(userRepository.findByIdForUpdate(1L)).thenReturn(activeUser)

        val group = mock(Group::class.java)
        `when`(group.id).thenReturn(10L)
        val membership = mock(GroupMember::class.java)
        `when`(membership.group).thenReturn(group)
        `when`(groupMemberRepository.findAllByUserIdAndWithdrawnAtIsNull(1L)).thenReturn(listOf(membership))

        `when`(groupService.delegateOwnerOrDeleteGroup(10L, 1L))
            .thenThrow(CannotAcquireLockException("lock wait timeout exceeded"))

        val exception = assertFailsWith<BusinessException> { userWithdrawalService.withdraw(1L) }

        assertEquals(UserErrorCode.WITHDRAWAL_CONFLICT, exception.errorCode)
    }

    @Test
    fun `여정 방장 위임 처리 중 락 경쟁이 발생하면 탈퇴 처리 충돌로 변환한다`() {
        val activeUser = mock(User::class.java)
        `when`(activeUser.isActive).thenReturn(true)
        `when`(userRepository.findByIdForUpdate(1L)).thenReturn(activeUser)
        `when`(groupMemberRepository.findAllByUserIdAndWithdrawnAtIsNull(1L)).thenReturn(emptyList())

        `when`(meetingService.delegateOwnersForWithdrawal(1L))
            .thenThrow(CannotAcquireLockException("lock wait timeout exceeded"))

        val exception = assertFailsWith<BusinessException> { userWithdrawalService.withdraw(1L) }

        assertEquals(UserErrorCode.WITHDRAWAL_CONFLICT, exception.errorCode)
    }
}
