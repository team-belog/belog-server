package org.com.belog.user.controller

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonSuccessCode
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.controller.dto.CompleteOnboardingRequest
import org.com.belog.user.controller.dto.NicknameAvailabilityResponse
import org.com.belog.user.controller.swagger.UserSwagger
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
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

    @PostMapping("/me/onboarding")
    override fun completeOnboarding(
        authentication: JwtAuthenticationToken,
        @RequestBody request: CompleteOnboardingRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        val userId = authentication.token.userId()
        userService.completeOnboarding(
            userId = userId,
            profileImageObjectKey = createProfileImageObjectKey(userId, request.profileImageObjectKey),
            nickname = request.nickname,
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
        value: String,
    ): ProfileImageObjectKey =
        try {
            ProfileImageObjectKey.create(userId, value)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE_OBJECT_KEY, exception)
        }

    private fun Jwt.userId(): Long =
        subject
            ?.toLongOrNull()
            ?.takeIf { userId -> userId > 0 }
            ?: throw BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN)
}
