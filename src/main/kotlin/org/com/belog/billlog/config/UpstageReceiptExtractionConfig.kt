package org.com.belog.billlog.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(UpstageReceiptExtractionProperties::class)
class UpstageReceiptExtractionConfig {
    @Bean
    @Qualifier(UPSTAGE_RECEIPT_EXTRACTION_REST_CLIENT)
    fun upstageReceiptExtractionRestClient(
        builder: RestClient.Builder,
        properties: UpstageReceiptExtractionProperties,
    ): RestClient =
        builder
            .baseUrl("${properties.baseUrl.toString().trimEnd('/')}/")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.apiKey}")
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(properties.connectionTimeout)
                    setReadTimeout(properties.readTimeout)
                },
            ).build()

    companion object {
        const val UPSTAGE_RECEIPT_EXTRACTION_REST_CLIENT = "upstageReceiptExtractionRestClient"
    }
}
