package org.com.belog.group.controller

import jakarta.validation.Valid
import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.code.GroupSuccessCode
import org.com.belog.group.controller.dto.CreateGroupRequest
import org.com.belog.group.controller.dto.CreateGroupResponse
import org.com.belog.group.controller.dto.GroupCoverImageUploadUrlRequest
import org.com.belog.group.controller.dto.GroupCoverImageUploadUrlResponse
import org.com.belog.group.controller.swagger.GroupSwagger
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.service.GroupCoverImageService
import org.com.belog.group.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val groupCoverImageService: GroupCoverImageService,
) : GroupSwagger {
    @PostMapping("/cover-image/upload-url")
    override fun issueCoverImageUploadUrl(
        authentication: Authentication,
        @Valid @RequestBody request: GroupCoverImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<GroupCoverImageUploadUrlResponse>> {
        val upload =
            groupCoverImageService.issueUploadUrl(
                userId = authenticatedUserId(authentication),
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
        authentication: Authentication,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ResponseEntity<CommonResponse<CreateGroupResponse>> {
        val creatorId = authenticatedUserId(authentication)
        val createdGroup =
            groupService.createGroup(
                creatorId = creatorId,
                name = request.name,
                coverImageObjectKey = request.coverImageObjectKey?.let { value -> createCoverImageObjectKey(creatorId, value) },
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

    private fun authenticatedUserId(authentication: Authentication): Long =
        authentication.name
            .toLongOrNull()
            ?.takeIf { userId -> userId > 0 }
            ?: throw BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN)

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
