package com.projectfaust.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI projectFaustOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Faust API")
                        .description("Global Political Hierarchy &amp; Institutional Occupation Map API")
                        .version("1.0.0"));
    }
}
