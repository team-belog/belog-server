package org.com.belog.home.controller

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.home.code.HomeSuccessCode
import org.com.belog.home.controller.cursor.ActiveMeetingCursorCodec
import org.com.belog.home.controller.cursor.CompletedMeetingCursorCodec
import org.com.belog.home.controller.dto.response.ActiveMeetingListResponse
import org.com.belog.home.controller.dto.response.CompletedMeetingListResponse
import org.com.belog.home.controller.dto.response.HomeCalendarResponse
import org.com.belog.home.controller.swagger.HomeSwagger
import org.com.belog.home.service.HomeService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/v1/home")
class HomeController(
    private val homeService: HomeService,
) : HomeSwagger {
    @GetMapping("/calendar")
    override fun getCalendar(
        @LoginUserId userId: Long,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") yearMonth: YearMonth,
    ): ResponseEntity<CommonResponse<HomeCalendarResponse>> {
        val result = homeService.getCalendar(userId = userId, yearMonth = yearMonth)

        return ResponseEntity
            .status(HomeSuccessCode.HOME_CALENDAR_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    HomeSuccessCode.HOME_CALENDAR_RETRIEVED,
                    HomeCalendarResponse.from(result),
                ),
            )
    }

    @GetMapping("/meetings/active")
    override fun getActiveMeetings(
        @LoginUserId userId: Long,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<ActiveMeetingListResponse>> {
        val result =
            homeService.getActiveMeetings(
                userId = userId,
                query = query,
                cursor = ActiveMeetingCursorCodec.decode(cursor),
                size = size,
            )

        return ResponseEntity
            .status(HomeSuccessCode.HOME_ACTIVE_MEETINGS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    HomeSuccessCode.HOME_ACTIVE_MEETINGS_RETRIEVED,
                    ActiveMeetingListResponse.from(result),
                ),
            )
    }

    @GetMapping("/meetings/completed")
    override fun getCompletedMeetings(
        @LoginUserId userId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<CompletedMeetingListResponse>> {
        val result =
            homeService.getCompletedMeetings(
                userId = userId,
                cursor = CompletedMeetingCursorCodec.decode(cursor),
                size = size,
            )

        return ResponseEntity
            .status(HomeSuccessCode.HOME_COMPLETED_MEETINGS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    HomeSuccessCode.HOME_COMPLETED_MEETINGS_RETRIEVED,
                    CompletedMeetingListResponse.from(result),
                ),
            )
    }
}
