package com.gateway.project.authservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {
    // Enables @Scheduled in RefreshTokenService (nightly token cleanup)
}
