package org.com.belog.billlog.controller

import org.com.belog.billlog.controller.swagger.SettlementRequestSwagger
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.annotation.LoginUserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log/settlement-requests")
class SettlementRequestController(
    private val settlementRequestService: SettlementRequestService,
) : SettlementRequestSwagger {
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
