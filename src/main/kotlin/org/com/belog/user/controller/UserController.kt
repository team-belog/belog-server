package org.com.belog.user.controller

import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonSuccessCode
import org.com.belog.user.controller.dto.NicknameAvailabilityResponse
import org.com.belog.user.controller.swagger.UserSwagger
import org.com.belog.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
}
