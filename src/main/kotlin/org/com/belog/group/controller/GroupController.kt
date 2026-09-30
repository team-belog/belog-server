package org.com.belog.group.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.code.GroupSuccessCode
import org.com.belog.group.controller.cursor.GroupListCursorCodec
import org.com.belog.group.controller.dto.request.CreateGroupRequest
import org.com.belog.group.controller.dto.request.GroupCoverImageUploadUrlRequest
import org.com.belog.group.controller.dto.request.UpdateGroupCoverImageRequest
import org.com.belog.group.controller.dto.response.CreateGroupResponse
import org.com.belog.group.controller.dto.response.GroupCoverImageUploadUrlResponse
import org.com.belog.group.controller.dto.response.GroupDetailResponse
import org.com.belog.group.controller.dto.response.GroupMembersResponse
import org.com.belog.group.controller.dto.response.MyGroupListResponse
import org.com.belog.group.controller.dto.response.PastMeetingListResponse
import org.com.belog.group.controller.swagger.GroupSwagger
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.service.GroupCoverImageService
import org.com.belog.group.service.GroupMembershipService
import org.com.belog.group.service.GroupService
import org.com.belog.meeting.code.MeetingSuccessCode
import org.com.belog.meeting.service.MeetingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val groupCoverImageService: GroupCoverImageService,
    private val groupMembershipService: GroupMembershipService,
    private val meetingService: MeetingService,
) : GroupSwagger {
    @GetMapping
    override fun getMyGroups(
        @LoginUserId userId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<CommonResponse<MyGroupListResponse>> {
        val result =
            groupService.getMyGroups(
                userId = userId,
                cursor = GroupListCursorCodec.decode(cursor),
                size = size,
            )

        return ResponseEntity
            .status(GroupSuccessCode.MY_GROUPS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.MY_GROUPS_RETRIEVED,
                    MyGroupListResponse.from(result),
                ),
            )
    }

    @PutMapping("/{groupId}/pin")
    override fun pinGroup(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<Void> {
        groupService.pinGroup(groupId = groupId, userId = userId)

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{groupId}/pin")
    override fun unpinGroup(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<Void> {
        groupService.unpinGroup(groupId = groupId, userId = userId)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{groupId}")
    override fun getGroup(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<CommonResponse<GroupDetailResponse>> {
        val group =
            groupService.getGroup(
                groupId = groupId,
                userId = userId,
            )

        return ResponseEntity
            .status(GroupSuccessCode.GROUP_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    GroupSuccessCode.GROUP_RETRIEVED,
                    GroupDetailResponse.from(group),
                ),
            )
    }

    @GetMapping("/{groupId}/meetings/past")
    override fun getPastMeetings(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<CommonResponse<PastMeetingListResponse>> {
        val result =
            meetingService.getPastMeetings(
                groupId = groupId,
                userId = userId,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(MeetingSuccessCode.PAST_MEETINGS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.PAST_MEETINGS_RETRIEVED,
                    PastMeetingListResponse.from(result),
                ),
            )
    }

    @GetMapping("/{groupId}/members")
    override fun getGroupMembers(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @RequestParam(required = false) query: String?,
    ): ResponseEntity<CommonResponse<GroupMembersResponse>> {
        val members =
            groupMembershipService.getGroupMembers(
                groupId = groupId,
                userId = userId,
                query = query,
            )

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

    @PutMapping("/{groupId}/cover-image")
    override fun updateCoverImage(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupCoverImageRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        groupCoverImageService.updateCoverImage(
            groupId = groupId,
            userId = userId,
            coverImageObjectKeyValue = request.coverImageObjectKey,
        )

        return ResponseEntity
            .status(GroupSuccessCode.GROUP_COVER_IMAGE_UPDATED.status)
            .body(CommonResponse.success(GroupSuccessCode.GROUP_COVER_IMAGE_UPDATED))
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
