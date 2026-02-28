package com.supportops.api.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    private String jwtSecret;
    private long accessTokenTtlSeconds = 900;
    private long refreshTokenTtlSeconds = 604800;
    private boolean cookieSecure;
    private String cookieSameSite = "Lax";
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
