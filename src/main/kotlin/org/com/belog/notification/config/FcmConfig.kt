package org.com.belog.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.ByteArrayInputStream
import java.util.Base64

@Configuration
@Profile("!test & !local")
@EnableConfigurationProperties(FcmProperties::class)
class FcmConfig {
    @Bean
    fun firebaseApp(properties: FcmProperties): FirebaseApp {
        val credentials = Base64.getDecoder().decode(properties.credentialsBase64)
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(credentials)))
                .setProjectId(properties.projectId)
                .build()

        return FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(options)
    }
}
