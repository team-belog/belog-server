package org.com.belog.notification.domain

enum class NotificationTargetType(
    val requiresId: Boolean = true,
) {
    HOME(requiresId = false),
    GROUP_HOME,
    DATE_POLL_RESPONSE,
    DATE_POLL_STATUS,
    MEETING_DETAIL,
    PRE_LOG,
    BILL_LOG,
    POST_LOG,
}
