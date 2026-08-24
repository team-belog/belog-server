package org.com.belog.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI belogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Belog API")
                        .description("Belog 서비스 API 문서")
                        .version("v1"));
    }
}
