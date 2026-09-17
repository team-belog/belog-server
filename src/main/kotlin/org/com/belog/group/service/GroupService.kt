package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.infrastructure.GroupInviteLinkGenerator
import org.com.belog.group.infrastructure.RandomInviteCodeGenerator
import org.com.belog.group.infrastructure.S3GroupCoverImageObjectVerifier
import org.com.belog.group.service.result.CreatedGroup
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val groupCreationAttemptService: GroupCreationAttemptService,
    private val inviteCodeGenerator: RandomInviteCodeGenerator,
    private val inviteLinkGenerator: GroupInviteLinkGenerator,
    private val groupCoverImageObjectVerifier: S3GroupCoverImageObjectVerifier,
) {
    fun createGroup(
        creatorId: Long,
        name: String,
        coverImageObjectKey: GroupCoverImageObjectKey?,
    ): CreatedGroup {
        coverImageObjectKey?.let(groupCoverImageObjectVerifier::verify)

        repeat(MAX_INVITE_CODE_ATTEMPTS) {
            val inviteCode = inviteCodeGenerator.generate()

            try {
                val group =
                    groupCreationAttemptService.create(
                        creatorId = creatorId,
                        name = name,
                        coverImageObjectKey = coverImageObjectKey,
                        inviteCode = inviteCode,
                    )

                return CreatedGroup(
                    groupId = requireNotNull(group.id),
                    name = group.name,
                    currentMemberCount = INITIAL_MEMBER_COUNT,
                    inviteCode = group.inviteCode,
                    inviteLink = inviteLinkGenerator.generate(inviteCode),
                )
            } catch (exception: DataIntegrityViolationException) {
                if (!exception.isInviteCodeUniqueConstraintViolation()) {
                    throw exception
                }
            }
        }

        throw BusinessException(GroupErrorCode.INVITE_CODE_ISSUANCE_FAILED)
    }

    private fun DataIntegrityViolationException.isInviteCodeUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }

    companion object {
        const val MAX_INVITE_CODE_ATTEMPTS = 5
        private const val INITIAL_MEMBER_COUNT = 1
    }
}
