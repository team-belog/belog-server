package org.com.belog.group.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.code.GroupSuccessCode
import org.com.belog.group.controller.dto.request.CreateGroupRequest
import org.com.belog.group.controller.dto.request.GroupCoverImageUploadUrlRequest
import org.com.belog.group.controller.dto.response.CreateGroupResponse
import org.com.belog.group.controller.dto.response.GroupCoverImageUploadUrlResponse
import org.com.belog.group.controller.dto.response.GroupMembersResponse
import org.com.belog.group.controller.swagger.GroupSwagger
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.service.GroupCoverImageService
import org.com.belog.group.service.GroupMembershipService
import org.com.belog.group.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val groupCoverImageService: GroupCoverImageService,
    private val groupMembershipService: GroupMembershipService,
) : GroupSwagger {
    @GetMapping("/{groupId}/members")
    override fun getGroupMembers(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<CommonResponse<GroupMembersResponse>> {
        val members = groupMembershipService.getGroupMembers(groupId, userId)

        return ResponseEntity
            .status(GroupSuccessCode.GROUP_MEMBERS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.GROUP_MEMBERS_RETRIEVED,
                    GroupMembersResponse.from(members),
                ),
            )
    }

    @PostMapping("/cover-image/upload-url")
    override fun issueCoverImageUploadUrl(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: GroupCoverImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<GroupCoverImageUploadUrlResponse>> {
        val upload =
            groupCoverImageService.issueUploadUrl(
                userId = userId,
                contentType = request.contentType,
                fileSize = request.fileSize,
            )

        return ResponseEntity
            .status(GroupSuccessCode.COVER_IMAGE_UPLOAD_URL_ISSUED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.COVER_IMAGE_UPLOAD_URL_ISSUED,
                    GroupCoverImageUploadUrlResponse.from(upload),
                ),
            )
    }

    @PostMapping
    override fun createGroup(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ResponseEntity<CommonResponse<CreateGroupResponse>> {
        val createdGroup =
            groupService.createGroup(
                creatorId = userId,
                name = request.name,
                coverImageObjectKey = request.coverImageObjectKey?.let { value -> createCoverImageObjectKey(userId, value) },
            )

        return ResponseEntity
            .status(GroupSuccessCode.GROUP_CREATED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.GROUP_CREATED,
                    CreateGroupResponse.from(createdGroup),
                ),
            )
    }

    private fun createCoverImageObjectKey(
        userId: Long,
        value: String,
    ): GroupCoverImageObjectKey =
        try {
            GroupCoverImageObjectKey.create(userId, value)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_OBJECT_KEY, exception)
        }
}
