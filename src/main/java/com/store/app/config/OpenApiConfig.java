package com.store.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation for the REST APIs. The Swagger UI is available
 * in development at /swagger-ui.html and disabled in the prod profile.
 * Protected endpoints authenticate with the JWT from POST /api/auth/login
 * via the Authorize button (Bearer scheme).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI storeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Store Management System API")
                        .description("REST APIs of the modular-monolith e-commerce and "
                                + "inventory management system. Authenticate via "
                                + "POST /api/auth/login and use the returned access token "
                                + "as a Bearer token.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
