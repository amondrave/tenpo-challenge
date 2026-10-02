package com.tenpo.challenge.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tenpoChallengeOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Tenpo Challenge API")
            .version("1.0.0")
            .description("Calculation and call history API for the Tenpo technical challenge"));
    }
}
