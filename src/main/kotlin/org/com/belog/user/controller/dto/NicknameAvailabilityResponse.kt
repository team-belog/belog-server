package org.com.belog.user.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

data class NicknameAvailabilityResponse(
    @field:Schema(description = "확인한 닉네임", example = "빌로그")
    val nickname: String,
    @field:Schema(description = "닉네임 사용 가능 여부", example = "true")
    val available: Boolean,
)
