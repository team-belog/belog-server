package org.com.belog.prelog.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

@Configuration
@EnableConfigurationProperties(KakaoMapsProperties::class)
class KakaoMapsConfig {
    @Bean(KAKAO_MAPS_HTTP_CLIENT)
    fun kakaoMapsHttpClient(properties: KakaoMapsProperties): HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(properties.connectionTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    companion object {
        const val KAKAO_MAPS_HTTP_CLIENT = "kakaoMapsHttpClient"
    }
}
