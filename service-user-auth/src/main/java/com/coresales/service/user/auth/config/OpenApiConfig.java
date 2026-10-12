package com.coresales.service.user.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el esquema JWT (bearerAuth) para que Swagger muestre el botón
 * Authorize. Solo /api/auth/sesion lo exige (@SecurityRequirement en el
 * controller); /login y /roles siguen siendo públicos.
 */
@Configuration
public class OpenApiConfig {

    public static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("service-user-auth")
                        .description("Autenticación (usuario de dominio/Windows) de Gestión de Requerimientos")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
