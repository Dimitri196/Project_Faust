package com.projectfaust.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for Project Faust.
 *
 * <p><b>Bearer auth scheme:</b> registers a JWT bearer security scheme named
 * {@code "bearerAuth"} and applies it globally to all endpoints. This is what
 * makes the "Authorize" padlock button appear in Swagger UI — without a
 * registered {@link SecurityScheme}, Swagger has nothing to attach an
 * authorization control to, even though the backend's JWT filter is fully
 * functional.</p>
 *
 * <p>Endpoints under {@code /api/v1/auth/**} remain publicly accessible per
 * {@link com.projectfaust.config.SecurityConfig} regardless of this global
 * requirement — {@code permitAll} at the filter-chain level takes precedence
 * over the documented security requirement.</p>
 *
 * @author Dimitri / Project Faust
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI projectFaustOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Faust API")
                        .description("Global Political Hierarchy &amp; Institutional Occupation Map API")
                        .version("1.0.0"))
                // Registers the "Authorize" button in Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token returned from POST /api/v1/auth/login. " +
                                        "Swagger automatically prefixes 'Bearer ' — paste only the token itself.")
                        ));
    }
}