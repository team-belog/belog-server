package org.com.belog.group.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.com.belog.group.controller.validation.ValidGroupName
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupCoverImageObjectKey

data class CreateGroupRequest(
    @field:Schema(
        description = "그룹명",
        example = "주말 러닝 모임",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = Group.NAME_MIN_LENGTH,
        maxLength = Group.NAME_MAX_LENGTH,
    )
    @field:ValidGroupName
    val name: String,
    @field:Schema(
        description = "미리 업로드한 그룹 커버 이미지의 S3 Object Key",
        example = "group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        maxLength = GroupCoverImageObjectKey.MAX_LENGTH,
    )
    @field:Size(
        max = GroupCoverImageObjectKey.MAX_LENGTH,
        message = "그룹 커버 이미지 object key는 ${GroupCoverImageObjectKey.MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    @field:Pattern(regexp = "^\\S+$", message = "그룹 커버 이미지 object key에는 공백을 포함할 수 없습니다.")
    val coverImageObjectKey: String? = null,
)
