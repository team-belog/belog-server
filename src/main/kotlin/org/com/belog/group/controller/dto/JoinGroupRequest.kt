package org.com.belog.group.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.group.domain.InviteCode

data class JoinGroupRequest(
    @field:Schema(
        description = "6자리 그룹 초대 코드",
        example = "AB12CD",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = InviteCode.LENGTH,
        maxLength = InviteCode.LENGTH,
    )
    @field:NotBlank(message = "초대 코드는 비어 있을 수 없습니다.")
    @field:Size(
        min = InviteCode.LENGTH,
        max = InviteCode.LENGTH,
        message = "초대 코드는 ${InviteCode.LENGTH}자리여야 합니다.",
    )
    val inviteCode: String,
)
