package org.com.belog.prelog.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
@EnableConfigurationProperties(GoogleMapsProperties::class)
class GoogleMapsConfig {
    @Bean
    fun googleMapsHttpClient(properties: GoogleMapsProperties): HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(properties.connectionTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    @Bean(GOOGLE_PLACES_REST_CLIENT)
    fun googlePlacesRestClient(
        builder: RestClient.Builder,
        @Qualifier(GOOGLE_MAPS_HTTP_CLIENT) httpClient: HttpClient,
        properties: GoogleMapsProperties,
    ): RestClient {
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(properties.readTimeout)

        return builder
            .baseUrl("https://places.googleapis.com")
            .requestFactory(requestFactory)
            .build()
    }

    companion object {
        const val GOOGLE_MAPS_HTTP_CLIENT = "googleMapsHttpClient"
        const val GOOGLE_PLACES_REST_CLIENT = "googlePlacesRestClient"
    }
}
