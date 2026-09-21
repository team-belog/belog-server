package org.com.belog.user.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonSuccessCode
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.code.UserSuccessCode
import org.com.belog.user.controller.dto.CompleteOnboardingRequest
import org.com.belog.user.controller.dto.NicknameAvailabilityResponse
import org.com.belog.user.controller.dto.ProfileImageUploadUrlRequest
import org.com.belog.user.controller.dto.ProfileImageUploadUrlResponse
import org.com.belog.user.controller.swagger.UserSwagger
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.service.ProfileImageService
import org.com.belog.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val profileImageService: ProfileImageService,
) : UserSwagger {
    @GetMapping("/nickname/availability")
    override fun checkNicknameAvailability(
        @RequestParam
        nickname: String,
    ): ResponseEntity<CommonResponse<NicknameAvailabilityResponse>> {
        val normalizedNickname = nickname.trim()
        val response =
            NicknameAvailabilityResponse(
                nickname = normalizedNickname,
                available = userService.isNicknameAvailable(normalizedNickname),
            )

        return ResponseEntity
            .status(CommonSuccessCode.OK.status)
            .body(CommonResponse.success(CommonSuccessCode.OK, response))
    }

    @PostMapping("/me/profile-image/upload-url")
    override fun issueProfileImageUploadUrl(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: ProfileImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<ProfileImageUploadUrlResponse>> {
        val upload =
            profileImageService.issueUploadUrl(
                userId = userId,
                contentType = request.contentType,
                fileSize = request.fileSize,
            )

        return ResponseEntity
            .status(UserSuccessCode.PROFILE_IMAGE_UPLOAD_URL_ISSUED.status)
            .body(
                CommonResponse.success(
                    UserSuccessCode.PROFILE_IMAGE_UPLOAD_URL_ISSUED,
                    ProfileImageUploadUrlResponse.from(upload),
                ),
            )
    }

    @PostMapping("/me/onboarding")
    override fun completeOnboarding(
        @LoginUserId userId: Long,
        @RequestBody request: CompleteOnboardingRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        userService.completeOnboarding(
            userId = userId,
            profileImageObjectKey = createProfileImageObjectKey(userId, request.profileImageObjectKey),
            nickname = request.nickname,
            name = request.name,
            bankAccount =
                BankAccount.create(
                    bank = requireNotNull(request.bankCode),
                    accountNumber = request.accountNumber,
                    accountHolderName = request.accountHolderName,
                ),
        )

        return ResponseEntity
            .status(CommonSuccessCode.OK.status)
            .body(CommonResponse.success(CommonSuccessCode.OK))
    }

    private fun createProfileImageObjectKey(
        userId: Long,
        value: String?,
    ): ProfileImageObjectKey? =
        value?.let {
            try {
                ProfileImageObjectKey.create(userId, it)
            } catch (exception: IllegalArgumentException) {
                throw BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE_OBJECT_KEY, exception)
            }
        }
}
