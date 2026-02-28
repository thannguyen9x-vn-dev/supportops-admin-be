package com.supportops.api;

import com.supportops.api.config.AppAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppAuthProperties.class)
public class SupportopsAdminBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportopsAdminBeApplication.class, args);
    }
}
