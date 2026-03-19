package com.supportops.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    private boolean enabled = false;
    private String provider = "smtp";
    private String from = "noreply@supportops.local";
    private String supportName = "SupportOps";
    private String sesRegion = "ap-southeast-1";
    private String passwordResetUrlTemplate = "http://localhost:3000/{locale}/reset-password?token={token}";
}
