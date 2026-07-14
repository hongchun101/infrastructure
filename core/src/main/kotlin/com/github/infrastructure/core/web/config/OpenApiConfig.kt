package com.github.infrastructure.core.web.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun infrastructureOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Infrastructure API")
                .description("Backend services exposed by the infrastructure platform")
                .version("v1"),
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("UUID")
                        .description("Bearer access token issued by /auth/login"),
                ),
        )
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}
