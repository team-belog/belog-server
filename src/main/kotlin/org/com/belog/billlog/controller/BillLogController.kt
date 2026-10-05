package org.com.belog.billlog.controller

import jakarta.validation.Valid
import org.com.belog.billlog.code.BillLogSuccessCode
import org.com.belog.billlog.controller.dto.request.ReceiptImageUploadUrlRequest
import org.com.belog.billlog.controller.dto.response.BillLogSummaryResponse
import org.com.belog.billlog.controller.dto.response.ReceiptImageUploadUrlResponse
import org.com.belog.billlog.controller.swagger.BillLogSwagger
import org.com.belog.billlog.service.BillLogService
import org.com.belog.billlog.service.ReceiptImageService
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/bill-log")
class BillLogController(
    private val billLogService: BillLogService,
    private val receiptImageService: ReceiptImageService,
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

    @PostMapping("/receipt-images/upload-url")
    override fun issueReceiptImageUploadUrl(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: ReceiptImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<ReceiptImageUploadUrlResponse>> {
        val upload =
            receiptImageService.issueUploadUrl(
                meetingId = meetingId,
                userId = userId,
                contentType = request.contentType,
                fileSize = request.fileSize,
            )

        return ResponseEntity
            .status(BillLogSuccessCode.RECEIPT_IMAGE_UPLOAD_URL_ISSUED.status)
            .body(
                CommonResponse.success(
                    BillLogSuccessCode.RECEIPT_IMAGE_UPLOAD_URL_ISSUED,
                    ReceiptImageUploadUrlResponse.from(upload),
                ),
            )
    }
}
