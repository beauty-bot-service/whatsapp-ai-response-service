package com.beautybot.whatsappairesponseservice.admin.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "beauty-bot.admin")
public class AdminProperties {

    private boolean enabled = true;
    private String bootstrapEmail;
    private String bootstrapPassword;
}
