package org.com.belog.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Google 로그인 결과")
data class GoogleLoginResponse(
    @field:Schema(description = "BELOG API Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "Access Token 만료까지 남은 시간(초)", example = "1800")
    val expiresIn: Long,
    @field:Schema(description = "온보딩 필요 여부", example = "true")
    val onboardingRequired: Boolean,
    @field:Schema(
        description = "사용자 지정 프로필 이미지가 없을 때 표시할 Google 프로필 이미지 URL",
        example = "https://lh3.googleusercontent.com/a/example",
        nullable = true,
    )
    val socialProfileImageUrl: String?,
)
