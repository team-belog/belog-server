package org.com.belog.user.service

import org.com.belog.auth.service.RefreshTokenService
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.error.BusinessException
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.service.GroupService
import org.com.belog.meeting.service.MeetingService
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class UserWithdrawalService(
    private val userRepository: UserRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService,
    private val meetingService: MeetingService,
    private val settlementRequestService: SettlementRequestService,
    private val refreshTokenService: RefreshTokenService,
    private val clock: Clock,
) {
    @Transactional
    fun withdraw(userId: Long) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        if (!user.isActive) {
            throw BusinessException(UserErrorCode.ALREADY_WITHDRAWN)
        }

        settlementRequestService.validateUserSettled(userId)

        val activeMemberships = groupMemberRepository.findAllByUserIdAndWithdrawnAtIsNull(userId)
        activeMemberships.forEach { membership ->
            groupService.delegateOwnerOrDeleteGroup(requireNotNull(membership.group.id), userId)
        }

        meetingService.delegateOwnersForWithdrawal(userId)

        val now = Instant.now(clock)
        activeMemberships.forEach { membership -> membership.withdraw(now) }
        user.withdraw(now)

        refreshTokenService.deleteByUserId(userId)
    }
}
