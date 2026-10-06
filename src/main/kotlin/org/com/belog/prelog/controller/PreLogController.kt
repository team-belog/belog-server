package org.com.belog.prelog.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.prelog.code.PreLogSuccessCode
import org.com.belog.prelog.controller.dto.request.CreatePlanRequest
import org.com.belog.prelog.controller.dto.request.UpdatePlanRequest
import org.com.belog.prelog.controller.dto.response.CreatePlanResponse
import org.com.belog.prelog.controller.dto.response.MapPlanListResponse
import org.com.belog.prelog.controller.dto.response.PlanLikeResponse
import org.com.belog.prelog.controller.dto.response.PlanListResponse
import org.com.belog.prelog.controller.dto.response.PreLogMainResponse
import org.com.belog.prelog.controller.dto.response.UpdatePlanResponse
import org.com.belog.prelog.controller.swagger.PreLogSwagger
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.PlanLikeService
import org.com.belog.prelog.service.PlanLinkService
import org.com.belog.prelog.service.PlanService
import org.com.belog.prelog.service.PreLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PreLogController(
    private val preLogService: PreLogService,
    private val planService: PlanService,
    private val planLinkService: PlanLinkService,
    private val planLikeService: PlanLikeService,
) : PreLogSwagger {
    @GetMapping("/api/v1/meetings/{meetingId}/pre-log")
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

    @GetMapping("/api/v1/meetings/{meetingId}/pre-log/map-plans")
    override fun getMapPlans(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @RequestParam(required = false) category: PlanCategory?,
        @RequestParam(required = false) @Positive cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<MapPlanListResponse>> {
        val result =
            planService.getMapPlans(
                meetingId = meetingId,
                userId = userId,
                category = category,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(PreLogSuccessCode.MAP_PLAN_LIST_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.MAP_PLAN_LIST_RETRIEVED,
                    MapPlanListResponse.from(result),
                ),
            )
    }

    @GetMapping("/api/v1/meetings/{meetingId}/pre-log/plans")
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

    @PostMapping("/api/v1/meetings/{meetingId}/pre-log/plans")
    override fun createPlan(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreatePlanRequest,
    ): ResponseEntity<CommonResponse<CreatePlanResponse>> {
        val plan =
            when (request.type) {
                PlanType.LINK ->
                    planLinkService.createLinkPlan(
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

    @PutMapping("/api/v1/plans/{planId}")
    override fun updatePlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
        @Valid @RequestBody request: UpdatePlanRequest,
    ): ResponseEntity<CommonResponse<UpdatePlanResponse>> {
        val plan =
            when (request.type) {
                PlanType.LINK ->
                    planLinkService.updateLinkPlan(
                        planId = planId,
                        userId = userId,
                        category = request.category,
                        title = request.title,
                        url = checkNotNull(request.url),
                    )

                PlanType.MEMO ->
                    planService.updateMemoPlan(
                        planId = planId,
                        userId = userId,
                        category = request.category,
                        title = request.title,
                        content = checkNotNull(request.content),
                    )
            }

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_UPDATED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PLAN_UPDATED,
                    UpdatePlanResponse.from(plan),
                ),
            )
    }

    @DeleteMapping("/api/v1/plans/{planId}")
    override fun deletePlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
    ): ResponseEntity<CommonResponse<Nothing>> {
        planService.deletePlan(planId = planId, userId = userId)

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_DELETED.status)
            .build()
    }

    @PutMapping("/api/v1/plans/{planId}/pin")
    override fun pinPlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
    ): ResponseEntity<CommonResponse<Nothing>> {
        planService.pinPlan(planId = planId, userId = userId)

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_PINNED.status)
            .body(CommonResponse.success(PreLogSuccessCode.PLAN_PINNED))
    }

    @DeleteMapping("/api/v1/plans/{planId}/pin")
    override fun unpinPlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
    ): ResponseEntity<CommonResponse<Nothing>> {
        planService.unpinPlan(planId = planId, userId = userId)

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_UNPINNED.status)
            .body(CommonResponse.success(PreLogSuccessCode.PLAN_UNPINNED))
    }

    @PutMapping("/api/v1/plans/{planId}/likes/me")
    override fun likePlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
    ): ResponseEntity<CommonResponse<PlanLikeResponse>> {
        val result =
            planLikeService.likePlan(
                planId = planId,
                userId = userId,
            )

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_LIKED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PLAN_LIKED,
                    PlanLikeResponse.from(result),
                ),
            )
    }

    @DeleteMapping("/api/v1/plans/{planId}/likes/me")
    override fun unlikePlan(
        @LoginUserId userId: Long,
        @PathVariable planId: Long,
    ): ResponseEntity<CommonResponse<PlanLikeResponse>> {
        val result =
            planLikeService.unlikePlan(
                planId = planId,
                userId = userId,
            )

        return ResponseEntity
            .status(PreLogSuccessCode.PLAN_LIKE_CANCELED.status)
            .body(
                CommonResponse.success(
                    PreLogSuccessCode.PLAN_LIKE_CANCELED,
                    PlanLikeResponse.from(result),
                ),
            )
    }
}
