package org.com.belog.home.controller

import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.home.code.HomeSuccessCode
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
}
