package org.com.belog.group.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.code.GroupSuccessCode
import org.com.belog.group.controller.dto.request.JoinGroupRequest
import org.com.belog.group.controller.dto.response.JoinGroupResponse
import org.com.belog.group.controller.swagger.GroupMembershipSwagger
import org.com.belog.group.service.GroupMembershipService
import org.springframework.http.ResponseEntity
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
        @LoginUserId userId: Long,
        @Valid @RequestBody request: JoinGroupRequest,
    ): ResponseEntity<CommonResponse<JoinGroupResponse>> {
        val joinedGroup =
            groupMembershipService.joinGroup(
                userId = userId,
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
}
