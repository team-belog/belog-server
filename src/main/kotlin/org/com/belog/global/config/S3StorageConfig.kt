package org.com.belog.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@EnableConfigurationProperties(S3StorageProperties::class)
class S3StorageConfig {
    @Bean
    fun s3Client(properties: S3StorageProperties): S3Client =
        S3Client
            .builder()
            .region(Region.of(properties.region))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()

    @Bean
    fun s3Presigner(properties: S3StorageProperties): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(properties.region))
            .build()
}
