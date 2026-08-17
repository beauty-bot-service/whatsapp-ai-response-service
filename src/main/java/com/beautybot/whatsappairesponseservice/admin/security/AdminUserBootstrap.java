package com.beautybot.whatsappairesponseservice.admin.security;

import com.beautybot.whatsappairesponseservice.application.support.ClinicIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "beauty-bot.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserBootstrap implements ApplicationRunner {

    private final AdminProperties properties;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClinicIdProvider clinicIdProvider;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!hasText(properties.getBootstrapEmail()) || !hasText(properties.getBootstrapPassword())) {
            log.info("Admin bootstrap credentials are not configured. Existing admin users remain available.");
            return;
        }
        if (properties.getBootstrapPassword().length() < 12) {
            throw new IllegalStateException("BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD must contain at least 12 characters.");
        }

        String email = properties.getBootstrapEmail().trim().toLowerCase(Locale.ROOT);
        if (adminUserRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Instant now = clock.instant();
        adminUserRepository.save(AdminUserEntity.builder()
                .clinicId(clinicIdProvider.currentClinicId())
                .email(email)
                .passwordHash(passwordEncoder.encode(properties.getBootstrapPassword()))
                .role(AdminRole.ADMIN)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        log.info("Initial admin user created for clinicId={}. Remove bootstrap credentials after provisioning.",
                clinicIdProvider.currentClinicId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
