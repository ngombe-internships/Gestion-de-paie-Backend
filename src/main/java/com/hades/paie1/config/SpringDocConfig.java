package com.hades.paie1.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("paie-api")
                .pathsToMatch("/api/**")
                .packagesToScan("com.hades.paie1.controller")
                .build();
    }
}
