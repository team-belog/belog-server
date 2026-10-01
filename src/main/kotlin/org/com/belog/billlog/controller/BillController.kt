package org.com.belog.billlog.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.com.belog.billlog.code.BillLogSuccessCode
import org.com.belog.billlog.controller.dto.request.RegisterBillRequest
import org.com.belog.billlog.controller.dto.response.BillDetailResponse
import org.com.belog.billlog.controller.dto.response.BillListResponse
import org.com.belog.billlog.controller.dto.response.RegisterBillResponse
import org.com.belog.billlog.controller.swagger.BillSwagger
import org.com.belog.billlog.service.BillLogService
import org.com.belog.billlog.service.BillService
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class BillController(
    private val billService: BillService,
    private val billLogService: BillLogService,
) : BillSwagger {
    @GetMapping("/api/v1/meetings/{meetingId}/bill-log/bills")
    override fun getBills(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        cursorDate: LocalDate?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<BillListResponse>> {
        val result =
            billLogService.getBills(
                meetingId = meetingId,
                userId = userId,
                cursorDate = cursorDate,
                size = size,
            )

        return ResponseEntity
            .status(BillLogSuccessCode.BILL_LIST_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.BILL_LIST_RETRIEVED,
                    BillListResponse.from(result),
                ),
            )
    }

    @GetMapping("/api/v1/bills/{billId}")
    override fun getBillDetail(
        @LoginUserId userId: Long,
        @PathVariable billId: Long,
    ): ResponseEntity<CommonResponse<BillDetailResponse>> {
        val billDetail =
            billService.getBillDetail(
                billId = billId,
                userId = userId,
            )

        return ResponseEntity
            .status(BillLogSuccessCode.BILL_DETAIL_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.BILL_DETAIL_RETRIEVED,
                    BillDetailResponse.from(billDetail),
                ),
            )
    }

    @PostMapping("/api/v1/meetings/{meetingId}/bill-log/bills")
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
