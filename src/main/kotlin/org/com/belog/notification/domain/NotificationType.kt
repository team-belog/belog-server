package org.com.belog.notification.domain

enum class NotificationType(
    val targetType: NotificationTargetType,
) {
    GROUP_MEMBER_JOINED(NotificationTargetType.GROUP_HOME),
    GROUP_DELETED(NotificationTargetType.HOME),
    MEETING_PROPOSED(NotificationTargetType.GROUP_HOME),
    DATE_POLL_STARTED(NotificationTargetType.DATE_POLL_RESPONSE),
    DATE_POLL_RESPONDED(NotificationTargetType.DATE_POLL_STATUS),
    DATE_POLL_REMINDER(NotificationTargetType.DATE_POLL_RESPONSE),
    MEETING_DATE_CONFIRMED(NotificationTargetType.MEETING_DETAIL),
    PRE_LOG_PLAN_CREATED(NotificationTargetType.PRE_LOG),
    PRE_LOG_PLAN_LIKED(NotificationTargetType.PRE_LOG),
    SETTLEMENT_REQUESTED(NotificationTargetType.BILL_LOG),
    BILL_REGISTERED(NotificationTargetType.BILL_LOG),
    SETTLEMENT_COMPLETED(NotificationTargetType.BILL_LOG),
    SETTLEMENT_REMINDER(NotificationTargetType.BILL_LOG),
    POST_LOG_PHOTOS_REGISTERED(NotificationTargetType.POST_LOG),
    POST_LOG_REVIEW_CREATED(NotificationTargetType.POST_LOG),
    MEETING_D7_REMINDER(NotificationTargetType.MEETING_DETAIL),
    PRE_LOG_D2_REMINDER(NotificationTargetType.PRE_LOG),
    POST_LOG_TODAY_REMINDER(NotificationTargetType.POST_LOG),
    GROUP_INACTIVE_60_DAYS(NotificationTargetType.GROUP_HOME),
}
