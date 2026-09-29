package org.com.belog.billlog.controller

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.com.belog.billlog.code.BillLogSuccessCode
import org.com.belog.billlog.controller.dto.response.SettlementRequestListResponse
import org.com.belog.billlog.controller.swagger.SettlementRequestSwagger
import org.com.belog.billlog.service.BillLogService
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log/settlement-requests")
class SettlementRequestController(
    private val billLogService: BillLogService,
    private val settlementRequestService: SettlementRequestService,
) : SettlementRequestSwagger {
    @GetMapping
    override fun getSettlementRequests(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<SettlementRequestListResponse>> {
        val result =
            billLogService.getSettlementRequests(
                meetingId = meetingId,
                userId = userId,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(BillLogSuccessCode.SETTLEMENT_REQUEST_LIST_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.SETTLEMENT_REQUEST_LIST_RETRIEVED,
                    SettlementRequestListResponse.from(result),
                ),
            )
    }

    @PutMapping("/{settlementRequestId}/completion")
    override fun complete(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable settlementRequestId: Long,
    ): ResponseEntity<Void> {
        settlementRequestService.complete(
            meetingId = meetingId,
            settlementRequestId = settlementRequestId,
            requesterUserId = userId,
        )

        return ResponseEntity.noContent().build()
    }
}
