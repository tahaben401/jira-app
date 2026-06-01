package com.gateway.project.sprintservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInternalAuthConfig {
    @Value("${internal.service-token}")
    private String token;

    @Bean
    public RequestInterceptor internalServiceAuthInterceptor() {
        return template -> template.header("X-Internal-Service-Token", token);
    }
}
