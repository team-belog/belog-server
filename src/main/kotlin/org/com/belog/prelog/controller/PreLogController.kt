package org.com.belog.prelog.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.prelog.code.PreLogSuccessCode
import org.com.belog.prelog.controller.dto.request.CreatePlanRequest
import org.com.belog.prelog.controller.dto.response.CreatePlanResponse
import org.com.belog.prelog.controller.dto.response.PlanListResponse
import org.com.belog.prelog.controller.dto.response.PreLogMainResponse
import org.com.belog.prelog.controller.swagger.PreLogSwagger
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.PlanService
import org.com.belog.prelog.service.PreLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/pre-log")
class PreLogController(
    private val preLogService: PreLogService,
    private val planService: PlanService,
) : PreLogSwagger {
    @GetMapping
    override fun getPreLogMain(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<PreLogMainResponse>> {
        val result = preLogService.getPreLogMain(meetingId = meetingId, userId = userId)

        return ResponseEntity
            .status(PreLogSuccessCode.PRE_LOG_MAIN_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PRE_LOG_MAIN_RETRIEVED,
                    PreLogMainResponse.from(result),
                ),
            )
    }

    @GetMapping("/plans")
    override fun getPlans(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @RequestParam(required = false) category: PlanCategory?,
        @RequestParam(defaultValue = "false") pinnedOnly: Boolean,
        @RequestParam(required = false) @Positive cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<PlanListResponse>> {
        val result =
            planService.getPlans(
                meetingId = meetingId,
                userId = userId,
                category = category,
                pinnedOnly = pinnedOnly,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_LIST_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PLAN_LIST_RETRIEVED,
                    PlanListResponse.from(result),
                ),
            )
    }

    @PostMapping("/plans")
    override fun createPlan(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreatePlanRequest,
    ): ResponseEntity<CommonResponse<CreatePlanResponse>> {
        val plan =
            when (request.type) {
                PlanType.LINK ->
                    planService.createLinkPlan(
                        meetingId = meetingId,
                        creatorUserId = userId,
                        category = request.category,
                        title = request.title,
                        url = checkNotNull(request.url),
                    )

                PlanType.MEMO ->
                    planService.createMemoPlan(
                        meetingId = meetingId,
                        creatorUserId = userId,
                        category = request.category,
                        title = request.title,
                        content = checkNotNull(request.content),
                    )
            }

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_CREATED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PLAN_CREATED,
                    CreatePlanResponse.from(plan),
                ),
            )
    }
}
