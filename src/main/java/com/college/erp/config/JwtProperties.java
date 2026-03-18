package com.college.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "erp.jwt")
@Data
public class JwtProperties {
    private String secret;
    private Long expirationMs;
    private String cookieName;
}
