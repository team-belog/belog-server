package org.com.belog.notification.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test | local")
class LoggingPushNotificationSender : PushNotificationSender {
    override fun send(
        fcmTokens: List<String>,
        payload: PushNotificationPayload,
    ): List<PushSendResult> {
        if (fcmTokens.isEmpty()) {
            return emptyList()
        }

        log.info(
            "FCM 발송을 생략하고 로그로 대체합니다. notificationId={}, type={}, deviceCount={}",
            payload.notificationId,
            payload.type,
            fcmTokens.size,
        )

        return fcmTokens.map { token -> PushSendResult(fcmToken = token, outcome = PushSendOutcome.SUCCESS) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LoggingPushNotificationSender::class.java)
    }
}
