package org.com.belog.notification.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.notification.domain.NotificationTargetType
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.result.NotificationListItemResult
import org.com.belog.notification.service.result.NotificationListResult
import org.com.belog.notification.service.result.NotificationTargetResult
import java.time.Instant

@Schema(description = "알림 목록 조회 결과")
data class NotificationListResponse(
    @field:Schema(description = "알림 목록")
    val items: List<NotificationListItemResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true, example = "100")
    val nextCursor: Long?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: NotificationListResult): NotificationListResponse =
            NotificationListResponse(
                items = result.items.map(NotificationListItemResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "알림 목록 항목")
data class NotificationListItemResponse(
    @field:Schema(description = "알림 ID", example = "101")
    val notificationId: Long,
    @field:Schema(description = "알림 타입", example = "SETTLEMENT_REQUESTED")
    val type: NotificationType,
    @field:Schema(description = "알림 문구", example = "피놀 님이 정산을 요청했어요")
    val message: String,
    @field:Schema(description = "알림 클릭 시 이동 대상")
    val target: NotificationTargetResponse,
    @field:Schema(description = "읽음 여부", example = "false")
    val read: Boolean,
    @field:Schema(description = "알림 생성 시각", example = "2026-10-01T04:30:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(result: NotificationListItemResult): NotificationListItemResponse =
            NotificationListItemResponse(
                notificationId = result.notificationId,
                type = result.type,
                message = result.message,
                target = NotificationTargetResponse.from(result.target),
                read = result.read,
                createdAt = result.createdAt,
            )
    }
}

@Schema(description = "알림 클릭 시 이동 대상")
data class NotificationTargetResponse(
    @field:Schema(description = "이동할 화면 타입", example = "BILL_LOG")
    val type: NotificationTargetType,
    @field:Schema(description = "화면 조회에 사용하는 대표 리소스 ID. HOME은 null", nullable = true, example = "7")
    val id: Long?,
) {
    companion object {
        fun from(result: NotificationTargetResult): NotificationTargetResponse =
            NotificationTargetResponse(
                type = result.type,
                id = result.id,
            )
    }
}
