package org.com.belog.prelog.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.prelog.code.PreLogSuccessCode
import org.com.belog.prelog.controller.dto.request.CreatePlanRequest
import org.com.belog.prelog.controller.dto.response.CreatePlanResponse
import org.com.belog.prelog.controller.swagger.PlanSwagger
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.PlanService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/pre-log/plans")
class PlanController(
    private val planService: PlanService,
) : PlanSwagger {
    @PostMapping
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
