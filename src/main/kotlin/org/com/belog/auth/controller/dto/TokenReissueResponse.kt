package org.com.belog.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 재발급 결과")
data class TokenReissueResponse(
    @field:Schema(description = "새로운 BELOG API Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "Access Token 만료까지 남은 시간(초)", example = "1800")
    val expiresIn: Long,
)
