package org.com.belog.notification.infrastructure

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.google.firebase.messaging.Notification as FirebaseNotification

@Component
@Profile("!test & !local")
class FirebasePushNotificationSender(
    firebaseApp: FirebaseApp,
) : PushNotificationSender {
    private val firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

    override fun send(
        fcmTokens: List<String>,
        payload: PushNotificationPayload,
    ): List<PushSendResult> {
        if (fcmTokens.isEmpty()) {
            return emptyList()
        }

        return fcmTokens.chunked(MAX_TOKENS_PER_MULTICAST).flatMap { tokenBatch -> sendBatch(tokenBatch, payload) }
    }

    private fun sendBatch(
        tokens: List<String>,
        payload: PushNotificationPayload,
    ): List<PushSendResult> {
        val response = firebaseMessaging.sendEachForMulticast(buildMulticastMessage(tokens, payload))

        return tokens.zip(response.responses).map { (token, sendResponse) ->
            val outcome =
                if (sendResponse.isSuccessful) {
                    PushSendOutcome.SUCCESS
                } else {
                    classify(sendResponse.exception)
                }
            PushSendResult(fcmToken = token, outcome = outcome)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildMulticastMessage(
        tokens: List<String>,
        payload: PushNotificationPayload,
    ): MulticastMessage =
        MulticastMessage
            .builder()
            .addAllTokens(tokens)
            .setNotification(
                FirebaseNotification
                    .builder()
                    .setBody(payload.message)
                    .build(),
            ).putData(DATA_KEY_NOTIFICATION_ID, payload.notificationId.toString())
            .putData(DATA_KEY_TYPE, payload.type.name)
            .putData(DATA_KEY_TARGET_TYPE, payload.targetType.name)
            .putData(DATA_KEY_TARGET_ID, payload.targetId?.toString().orEmpty())
            .build()

    private fun classify(exception: FirebaseMessagingException?): PushSendOutcome =
        when (exception?.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT -> PushSendOutcome.INVALID_TOKEN
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.QUOTA_EXCEEDED,
            -> PushSendOutcome.RETRYABLE_FAILURE
            else -> PushSendOutcome.PERMANENT_FAILURE
        }

    companion object {
        private const val MAX_TOKENS_PER_MULTICAST = 500
        private const val DATA_KEY_NOTIFICATION_ID = "notificationId"
        private const val DATA_KEY_TYPE = "type"
        private const val DATA_KEY_TARGET_TYPE = "targetType"
        private const val DATA_KEY_TARGET_ID = "targetId"
    }
}
