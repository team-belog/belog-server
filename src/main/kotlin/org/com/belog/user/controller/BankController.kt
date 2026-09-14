package org.com.belog.user.controller

import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonSuccessCode
import org.com.belog.user.controller.dto.BankResponse
import org.com.belog.user.controller.swagger.BankSwagger
import org.com.belog.user.domain.Bank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/banks")
class BankController : BankSwagger {
    @GetMapping
    override fun getBanks(): ResponseEntity<CommonResponse<List<BankResponse>>> {
        val response = Bank.entries.map(BankResponse::from)

        return ResponseEntity
            .status(CommonSuccessCode.OK.status)
            .body(CommonResponse.success(CommonSuccessCode.OK, response))
    }
}
