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
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.user.controller.dto.request.CompleteOnboardingRequest
import org.com.belog.user.controller.dto.request.ProfileImageUploadUrlRequest
import org.com.belog.user.controller.dto.request.UpdateBankAccountRequest
import org.com.belog.user.controller.dto.response.BankAccountResponse
import org.com.belog.user.controller.dto.response.NicknameAvailabilityResponse
import org.com.belog.user.controller.dto.response.ProfileImageUploadUrlResponse
import org.com.belog.user.controller.dto.response.UserProfileResponse
import org.com.belog.user.controller.validation.ValidNickname
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "User", description = "사용자 관련 API")
interface UserSwagger {
    @Operation(
        summary = "내 프로필 조회",
        description =
            "로그인한 사용자의 닉네임과 표시용 프로필 이미지 URL을 조회합니다. " +
                "앱 기본 이미지를 사용하는 경우 profileImageUrl은 null입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "프로필 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(name = "프로필 이미지 있음", value = PROFILE_SUCCESS_EXAMPLE),
                            ExampleObject(name = "앱 기본 이미지 사용", value = DEFAULT_PROFILE_SUCCESS_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
            ApiResponse(
                responseCode = "403",
                description = "온보딩 미완료",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = ONBOARDING_REQUIRED_EXAMPLE)],
                    ),
                ],
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
    fun getProfile(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
    ): ResponseEntity<CommonResponse<UserProfileResponse>>

    @Operation(
        summary = "내 계좌 조회",
        description = "로그인한 사용자의 정산 계좌 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "계좌 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = BANK_ACCOUNT_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 또는 등록 계좌를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "사용자 없음", value = USER_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "등록 계좌 없음", value = BANK_ACCOUNT_NOT_REGISTERED_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getBankAccount(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
    ): ResponseEntity<CommonResponse<BankAccountResponse>>

    @Operation(
        summary = "내 계좌 수정",
        description = "로그인한 사용자의 정산 계좌 정보를 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "계좌 수정 성공",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 또는 등록 계좌를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "사용자 없음", value = USER_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "등록 계좌 없음", value = BANK_ACCOUNT_NOT_REGISTERED_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateBankAccount(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UpdateBankAccountRequest::class),
                    examples = [ExampleObject(value = UPDATE_BANK_ACCOUNT_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: UpdateBankAccountRequest,
    ): ResponseEntity<CommonResponse<Nothing>>

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
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = PROFILE_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패 또는 지원하지 않는 이미지",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
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
        @LoginUserId
        userId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ProfileImageUploadUrlRequest::class),
                    examples = [ExampleObject(value = PROFILE_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: ProfileImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<ProfileImageUploadUrlResponse>>

    @Operation(
        summary = "온보딩 완료",
        description = "프로필과 정산 계좌 정보를 저장하고 온보딩을 완료합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "온보딩 완료",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값, 프로필 이미지 object key 또는 업로드된 이미지 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 프로필 이미지 object key", value = INVALID_PROFILE_IMAGE_OBJECT_KEY_EXAMPLE),
                            ExampleObject(name = "업로드된 프로필 이미지 없음", value = PROFILE_IMAGE_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "잘못된 프로필 이미지 정보", value = INVALID_PROFILE_IMAGE_METADATA_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
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
            ApiResponse(
                responseCode = "409",
                description = "닉네임 중복 또는 이미 완료된 온보딩",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "닉네임 중복", value = NICKNAME_ALREADY_EXISTS_EXAMPLE),
                            ExampleObject(name = "이미 완료된 온보딩", value = ONBOARDING_ALREADY_COMPLETED_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun completeOnboarding(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = CompleteOnboardingRequest::class),
                    examples = [
                        ExampleObject(name = "Google 프로필 이미지 사용", value = ONBOARDING_WITH_GOOGLE_IMAGE_REQUEST_EXAMPLE),
                        ExampleObject(name = "직접 업로드한 이미지 사용", value = ONBOARDING_WITH_CUSTOM_IMAGE_REQUEST_EXAMPLE),
                    ],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: CompleteOnboardingRequest,
    ): ResponseEntity<CommonResponse<Nothing>>
}

const val NICKNAME_LENGTH_MESSAGE = "닉네임은 1자 이상 8자 이하여야 합니다."

private const val PROFILE_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE =
    """{"contentType":"image/webp","fileSize":524288}"""

private const val PROFILE_SUCCESS_EXAMPLE =
    """{"code":"USER-S002","message":"프로필을 조회했습니다.","data":{"nickname":"피블","profileImageUrl":"https://belog-profile.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp?..."}}"""

private const val DEFAULT_PROFILE_SUCCESS_EXAMPLE =
    """{"code":"USER-S002","message":"프로필을 조회했습니다.","data":{"nickname":"피블","profileImageUrl":null}}"""

private const val BANK_ACCOUNT_SUCCESS_EXAMPLE =
    """{"code":"CMN-S001","message":"요청이 성공했습니다.","data":{"bankCode":"SHINHAN","bankName":"신한은행","accountNumber":"110123456789","accountHolderName":"홍길동"}}"""

private const val UPDATE_BANK_ACCOUNT_REQUEST_EXAMPLE =
    """{"bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}"""

private const val PROFILE_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE =
    """{"code":"USER-S001","message":"프로필 이미지 업로드 URL이 발급되었습니다.","data":{"objectKey":"users/15/profile/550e8400-e29b-41d4-a716-446655440000.webp","uploadUrl":"https://belog-profile.s3.ap-northeast-2.amazonaws.com/users/15/profile/550e8400-e29b-41d4-a716-446655440000.webp?...","method":"PUT","requiredHeaders":{"Content-Type":"image/webp","Content-Length":"524288"},"expiresAt":"2026-09-14T14:05:00Z"}}"""

private const val ONBOARDING_WITH_GOOGLE_IMAGE_REQUEST_EXAMPLE =
    """{"nickname":"빌로그","name":"홍길동","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}"""

private const val ONBOARDING_WITH_CUSTOM_IMAGE_REQUEST_EXAMPLE =
    """{"profileImageObjectKey":"users/15/profile/550e8400-e29b-41d4-a716-446655440000.webp","nickname":"빌로그","name":"홍길동","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}"""

private const val INVALID_NICKNAME_LENGTH_EXAMPLE =
    """{"code":"CMN-E001","message":"요청값이 올바르지 않습니다.","data":{"fieldErrors":[{"field":"nickname","reason":"닉네임은 1자 이상 8자 이하여야 합니다."}],"timestamp":"2026-09-13T00:00:00Z"}}"""

private const val UNSUPPORTED_PROFILE_IMAGE_TYPE_EXAMPLE =
    """{"code":"USER-E005","message":"지원하지 않는 프로필 이미지 형식입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""

private const val INVALID_PROFILE_IMAGE_SIZE_EXAMPLE =
    """{"code":"USER-E006","message":"프로필 이미지는 5MB 이하여야 합니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""

private const val USER_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-14T00:00:00Z"}}"""

private const val BANK_ACCOUNT_NOT_REGISTERED_EXAMPLE =
    """{"code":"USER-E007","message":"등록된 계좌가 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val ONBOARDING_REQUIRED_EXAMPLE =
    """{"code":"USER-E008","message":"온보딩을 완료한 사용자만 이용할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val INVALID_PROFILE_IMAGE_OBJECT_KEY_EXAMPLE =
    """{"code":"USER-E004","message":"프로필 이미지 object key가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val PROFILE_IMAGE_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E009","message":"업로드된 프로필 이미지를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val INVALID_PROFILE_IMAGE_METADATA_EXAMPLE =
    """{"code":"USER-E010","message":"프로필 이미지 정보가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val NICKNAME_ALREADY_EXISTS_EXAMPLE =
    """{"code":"USER-E002","message":"이미 사용 중인 닉네임입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val ONBOARDING_ALREADY_COMPLETED_EXAMPLE =
    """{"code":"USER-E003","message":"이미 온보딩을 완료했습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""
