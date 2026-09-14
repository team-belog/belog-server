package org.com.belog.user.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.com.belog.global.config.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.user.controller.dto.BankResponse
import org.springframework.http.ResponseEntity

@Tag(name = "Bank", description = "은행 관련 API")
interface BankSwagger {
    @Operation(
        summary = "은행 목록 조회",
        description = "온보딩에서 선택할 수 있는 은행 코드와 표시 이름을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "은행 목록 조회 성공",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "401",
                ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED,
            ),
        ],
    )
    fun getBanks(): ResponseEntity<CommonResponse<List<BankResponse>>>
}
