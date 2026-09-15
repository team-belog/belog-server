package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GroupCreationAttemptService(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun create(
        creatorId: Long,
        name: String,
        coverImageObjectKey: GroupCoverImageObjectKey?,
        inviteCode: InviteCode,
    ): Group {
        val creator =
            userRepository.findById(creatorId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        if (!creator.isOnboardingCompleted) {
            throw BusinessException(GroupErrorCode.ONBOARDING_REQUIRED)
        }

        val group =
            groupRepository.save(
                Group.create(
                    name = name,
                    coverImageObjectKey = coverImageObjectKey,
                    inviteCode = inviteCode,
                ),
            )

        groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, creator))
        return group
    }
}
