package org.com.belog.billlog.controller

import jakarta.validation.Valid
import org.com.belog.billlog.controller.dto.request.UpdateSettlementRequestStatusRequest
import org.com.belog.billlog.controller.swagger.SettlementRequestSwagger
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.annotation.LoginUserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log/settlement-requests")
class SettlementRequestController(
    private val settlementRequestService: SettlementRequestService,
) : SettlementRequestSwagger {
    @PatchMapping("/{settlementRequestId}")
    override fun updateStatus(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable settlementRequestId: Long,
        @Valid @RequestBody request: UpdateSettlementRequestStatusRequest,
    ): ResponseEntity<Void> {
        settlementRequestService.updateStatus(
            meetingId = meetingId,
            settlementRequestId = settlementRequestId,
            payerUserId = userId,
            status = requireNotNull(request.status),
        )

        return ResponseEntity.noContent().build()
    }
}
