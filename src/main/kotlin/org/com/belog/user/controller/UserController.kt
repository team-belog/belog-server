package org.com.belog.user.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonSuccessCode
import org.com.belog.postlog.code.PostLogSuccessCode
import org.com.belog.postlog.service.PostLogCalendarService
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.code.UserSuccessCode
import org.com.belog.user.controller.dto.request.CompleteOnboardingRequest
import org.com.belog.user.controller.dto.request.ProfileImageUpdateType
import org.com.belog.user.controller.dto.request.ProfileImageUploadUrlRequest
import org.com.belog.user.controller.dto.request.UpdateBankAccountRequest
import org.com.belog.user.controller.dto.request.UpdateUserProfileRequest
import org.com.belog.user.controller.dto.response.BankAccountResponse
import org.com.belog.user.controller.dto.response.NicknameAvailabilityResponse
import org.com.belog.user.controller.dto.response.PostLogTicketCalendarResponse
import org.com.belog.user.controller.dto.response.ProfileImageUploadUrlResponse
import org.com.belog.user.controller.dto.response.UserProfileResponse
import org.com.belog.user.controller.swagger.UserSwagger
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.service.UserService
import org.com.belog.user.service.command.ProfileImageChange
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val postLogCalendarService: PostLogCalendarService,
) : UserSwagger {
    @GetMapping("/me/post-logs/calendar")
    override fun getMyTicketCalendar(
        @LoginUserId userId: Long,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") yearMonth: YearMonth,
    ): ResponseEntity<CommonResponse<PostLogTicketCalendarResponse>> {
        val result = postLogCalendarService.getMyTicketCalendar(userId, yearMonth)

        return ResponseEntity
            .status(PostLogSuccessCode.TICKET_CALENDAR_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.TICKET_CALENDAR_RETRIEVED,
                    PostLogTicketCalendarResponse.from(result),
                ),
            )
    }

    @GetMapping("/me/profile")
    override fun getProfile(
        @LoginUserId userId: Long,
    ): ResponseEntity<CommonResponse<UserProfileResponse>> {
        val response = UserProfileResponse.from(userService.getProfile(userId))

        return ResponseEntity
            .status(UserSuccessCode.PROFILE_RETRIEVED.status)
            .cacheControl(CacheControl.noStore())
            .body(CommonResponse.success(UserSuccessCode.PROFILE_RETRIEVED, response))
    }

    @PatchMapping("/me/profile")
    override fun updateProfile(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdateUserProfileRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        userService.updateProfile(
            userId = userId,
            nickname = request.nickname,
            profileImageChange = createProfileImageChange(userId, request),
        )

        return ResponseEntity
            .status(CommonSuccessCode.OK.status)
            .body(CommonResponse.success(CommonSuccessCode.OK))
    }

    @GetMapping("/me/bank-account")
    override fun getBankAccount(
        @LoginUserId userId: Long,
    ): ResponseEntity<CommonResponse<BankAccountResponse>> {
        val response = BankAccountResponse.from(userService.getBankAccount(userId))

        return ResponseEntity
            .status(CommonSuccessCode.OK.status)
            .cacheControl(CacheControl.noStore())
            .body(CommonResponse.success(CommonSuccessCode.OK, response))
    }

    @PutMapping("/me/bank-account")
    override fun updateBankAccount(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdateBankAccountRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        userService.updateBankAccount(
            userId = userId,
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
            userService.issueProfileImageUploadUrl(
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

    private fun createProfileImageChange(
        userId: Long,
        request: UpdateUserProfileRequest,
    ): ProfileImageChange? =
        when (request.profileImageType) {
            ProfileImageUpdateType.CUSTOM ->
                ProfileImageChange.Update(
                    requireNotNull(createProfileImageObjectKey(userId, request.profileImageObjectKey)),
                )
            ProfileImageUpdateType.DEFAULT -> ProfileImageChange.Reset
            null -> null
        }
}
