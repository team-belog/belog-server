package org.com.belog.user.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.com.belog.global.config.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.user.controller.dto.CompleteOnboardingRequest
import org.com.belog.user.controller.dto.NicknameAvailabilityResponse
import org.com.belog.user.controller.dto.ProfileImageUploadUrlRequest
import org.com.belog.user.controller.dto.ProfileImageUploadUrlResponse
import org.com.belog.user.controller.validation.ValidNickname
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

@Tag(name = "User", description = "사용자 관련 API")
interface UserSwagger {
    @Operation(
        summary = "닉네임 중복 확인",
        description = "1~8자의 닉네임이 사용 가능한지 확인합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "닉네임 중복 확인 성공",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "닉네임 길이 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INVALID_NICKNAME_LENGTH_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
        ],
    )
    fun checkNicknameAvailability(
        @Parameter(description = "확인할 닉네임", example = "빌로그")
        @ValidNickname(message = NICKNAME_LENGTH_MESSAGE)
        nickname: String,
    ): ResponseEntity<CommonResponse<NicknameAvailabilityResponse>>

    @Operation(
        summary = "프로필 이미지 업로드 URL 발급",
        description = "JPEG, PNG 또는 WebP 이미지를 S3에 직접 업로드할 수 있는 Presigned URL을 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "프로필 이미지 업로드 URL 발급 성공",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패 또는 지원하지 않는 이미지",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "지원하지 않는 이미지 형식", value = UNSUPPORTED_PROFILE_IMAGE_TYPE_EXAMPLE),
                            ExampleObject(name = "이미지 크기 초과", value = INVALID_PROFILE_IMAGE_SIZE_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun issueProfileImageUploadUrl(
        @Parameter(hidden = true)
        authentication: Authentication,
        @Valid @RequestBody request: ProfileImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<ProfileImageUploadUrlResponse>>

    @Operation(
        summary = "온보딩 완료",
        description = "프로필과 정산 계좌 정보를 저장하고 온보딩을 완료합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "온보딩 완료", useReturnTypeSchema = true),
            ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(responseCode = "409", description = "닉네임 중복 또는 이미 완료된 온보딩"),
        ],
    )
    fun completeOnboarding(
        @Parameter(hidden = true) authentication: JwtAuthenticationToken,
        @Valid @RequestBody request: CompleteOnboardingRequest,
    ): ResponseEntity<CommonResponse<Nothing>>
}

const val NICKNAME_LENGTH_MESSAGE = "닉네임은 1자 이상 8자 이하여야 합니다."

private const val INVALID_NICKNAME_LENGTH_EXAMPLE =
    """{"code":"CMN-E001","message":"요청값이 올바르지 않습니다.","data":{"fieldErrors":[{"field":"nickname","reason":"닉네임은 1자 이상 8자 이하여야 합니다."}],"timestamp":"2026-09-13T00:00:00Z"}}"""

private const val UNSUPPORTED_PROFILE_IMAGE_TYPE_EXAMPLE =
    """{"code":"USER-E002","message":"지원하지 않는 프로필 이미지 형식입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""

private const val INVALID_PROFILE_IMAGE_SIZE_EXAMPLE =
    """{"code":"USER-E003","message":"프로필 이미지는 5MB 이하여야 합니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""

private const val USER_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""
