package com.protean.security.aerith.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.protean.security.auron",
    "com.protean.security.aerith"})
public class ApplicationConfig {
}
