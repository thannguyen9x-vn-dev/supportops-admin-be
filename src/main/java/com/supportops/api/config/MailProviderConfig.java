package com.supportops.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class MailProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "ses")
    SesClient sesClient(AppMailProperties mailProperties) {
        return SesClient.builder()
            .region(Region.of(mailProperties.getSesRegion()))
            .build();
    }
}
