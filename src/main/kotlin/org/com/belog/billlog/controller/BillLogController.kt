package org.com.belog.billlog.controller

import org.com.belog.billlog.code.BillLogSuccessCode
import org.com.belog.billlog.controller.dto.response.BillLogSummaryResponse
import org.com.belog.billlog.controller.swagger.BillLogSwagger
import org.com.belog.billlog.service.BillLogService
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log")
class BillLogController(
    private val billLogService: BillLogService,
) : BillLogSwagger {
    @GetMapping
    override fun getSummary(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<BillLogSummaryResponse>> {
        val summary = billLogService.getSummary(meetingId = meetingId, userId = userId)

        return ResponseEntity
            .status(BillLogSuccessCode.BILL_LOG_SUMMARY_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.BILL_LOG_SUMMARY_RETRIEVED,
                    BillLogSummaryResponse.from(summary),
                ),
            )
    }
}
