package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GROUP_MEMBER_UNIQUE_CONSTRAINT_NAME
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.group.service.result.GroupMemberResult
import org.com.belog.group.service.result.JoinedGroup
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.ProfileImageService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMembershipService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val profileImageService: ProfileImageService,
) {
    @Transactional(readOnly = true)
    fun getGroupMembers(
        groupId: Long,
        userId: Long,
    ): List<GroupMemberResult> {
        validateGroupMember(groupId, userId)

        return groupMemberRepository.findAllWithUserByGroupId(groupId).map { member ->
            val memberUser = member.user
            val memberUserId = requireNotNull(memberUser.id)
            GroupMemberResult(
                groupMemberId = requireNotNull(member.id),
                nickname = requireNotNull(memberUser.nickname),
                profileImageUrl =
                    profileImageService.generateReadUrl(
                        userId = memberUserId,
                        profileImageObjectKey = memberUser.profileImageObjectKey,
                    ) ?: memberUser.socialProfileImageUrl,
                role = member.role,
            )
        }
    }

    @Transactional
    fun joinGroup(
        userId: Long,
        inviteCode: String,
    ): JoinedGroup {
        val validatedInviteCode = createInviteCode(inviteCode)
        val group =
            groupRepository.findByInviteCodeForUpdate(validatedInviteCode.value)
                ?: throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND_BY_INVITE_CODE)
        val groupId = requireNotNull(group.id)
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        if (!user.isOnboardingCompleted) {
            throw BusinessException(GroupErrorCode.ONBOARDING_REQUIRED)
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.ALREADY_GROUP_MEMBER)
        }

        val currentMemberCount = groupMemberRepository.countByGroupId(groupId)
        if (currentMemberCount >= Group.MAX_MEMBER_COUNT) {
            throw BusinessException(GroupErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED)
        }

        try {
            groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
        } catch (exception: DataIntegrityViolationException) {
            if (exception.isGroupMemberUniqueConstraintViolation()) {
                throw BusinessException(GroupErrorCode.ALREADY_GROUP_MEMBER, exception)
            }
            throw exception
        }

        return JoinedGroup(
            groupId = groupId,
            name = group.name,
            currentMemberCount = currentMemberCount.toInt() + 1,
        )
    }

    private fun validateGroupMember(
        groupId: Long,
        userId: Long,
    ) {
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, userId) != null) {
            return
        }
        if (!groupRepository.existsById(groupId)) {
            throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        }
        throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun createInviteCode(value: String): InviteCode =
        try {
            InviteCode.create(value)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(GroupErrorCode.INVALID_INVITE_CODE, exception)
        }

    private fun DataIntegrityViolationException.isGroupMemberUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(GROUP_MEMBER_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }
}
