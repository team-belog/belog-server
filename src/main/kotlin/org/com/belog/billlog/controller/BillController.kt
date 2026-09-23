package org.com.belog.billlog.controller

import jakarta.validation.Valid
import org.com.belog.billlog.code.BillLogSuccessCode
import org.com.belog.billlog.controller.dto.request.RegisterBillRequest
import org.com.belog.billlog.controller.dto.response.RegisterBillResponse
import org.com.belog.billlog.controller.swagger.BillSwagger
import org.com.belog.billlog.service.BillService
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log/bills")
class BillController(
    private val billService: BillService,
) : BillSwagger {
    @PostMapping
    override fun registerBill(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: RegisterBillRequest,
    ): ResponseEntity<CommonResponse<RegisterBillResponse>> {
        val registeredBill = billService.registerBill(request.toCommand(meetingId, userId))

        return ResponseEntity
            .status(BillLogSuccessCode.BILL_REGISTERED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.BILL_REGISTERED,
                    RegisterBillResponse.from(registeredBill),
                ),
            )
    }

    private fun RegisterBillRequest.toCommand(
        meetingId: Long,
        creatorUserId: Long,
    ): RegisterBillCommand =
        RegisterBillCommand(
            meetingId = meetingId,
            creatorUserId = creatorUserId,
            title = title,
            payerMemberId = payerMemberId,
            totalAmount = totalAmount,
            splitType = splitType,
            items = items.map { item -> BillItemCommand(name = item.name, amount = item.amount) },
            shares =
                shares.map { share ->
                    BillShareCommand(
                        participantMemberId = share.participantMemberId,
                        amount = share.amount,
                    )
                },
        )
}
