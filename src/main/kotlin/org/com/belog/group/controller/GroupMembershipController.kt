package org.com.belog.group.controller

import jakarta.validation.Valid
import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.code.GroupSuccessCode
import org.com.belog.group.controller.dto.JoinGroupRequest
import org.com.belog.group.controller.dto.JoinGroupResponse
import org.com.belog.group.controller.swagger.GroupMembershipSwagger
import org.com.belog.group.service.GroupMembershipService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/group-memberships")
class GroupMembershipController(
    private val groupMembershipService: GroupMembershipService,
) : GroupMembershipSwagger {
    @PostMapping
    override fun joinGroup(
        authentication: Authentication,
        @Valid @RequestBody request: JoinGroupRequest,
    ): ResponseEntity<CommonResponse<JoinGroupResponse>> {
        val joinedGroup =
            groupMembershipService.joinGroup(
                userId = authenticatedUserId(authentication),
                inviteCode = request.inviteCode,
            )

        return ResponseEntity
            .status(GroupSuccessCode.GROUP_JOINED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.GROUP_JOINED,
                    JoinGroupResponse.from(joinedGroup),
                ),
            )
    }

    private fun authenticatedUserId(authentication: Authentication): Long =
        authentication.name
            .toLongOrNull()
            ?.takeIf { userId -> userId > 0 }
            ?: throw BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN)
}
