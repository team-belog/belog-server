package org.com.belog.user.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import org.com.belog.global.config.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.user.controller.dto.NicknameAvailabilityResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

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
        @Size(min = NICKNAME_MIN_LENGTH, max = NICKNAME_MAX_LENGTH, message = NICKNAME_LENGTH_MESSAGE)
        nickname: String,
    ): ResponseEntity<CommonResponse<NicknameAvailabilityResponse>>
}

const val NICKNAME_MIN_LENGTH = 1
const val NICKNAME_MAX_LENGTH = 8
const val NICKNAME_LENGTH_MESSAGE = "닉네임은 1자 이상 8자 이하여야 합니다."

private const val INVALID_NICKNAME_LENGTH_EXAMPLE =
    """{"code":"CMN-E001","message":"요청값이 올바르지 않습니다.","data":{"fieldErrors":[{"field":"nickname","reason":"닉네임은 1자 이상 8자 이하여야 합니다."}],"timestamp":"2026-09-13T00:00:00Z"}}"""
