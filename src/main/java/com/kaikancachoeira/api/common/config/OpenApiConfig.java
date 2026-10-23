package com.kaikancachoeira.api.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido pelo endpoint de login"
)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kaikan Cachoeira API")
                        .description("API de gestão de eventos, estoque, doações, vendas e caixa do Kaikan de Cachoeira")
                        .version("0.0.1"))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                );
    }

}
