package org.com.belog.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    fun belogOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Belog API")
                .description("Belog 서비스 API 문서")
                .version("v1"),
            )
}
