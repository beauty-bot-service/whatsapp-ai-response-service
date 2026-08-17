package com.beautybot.whatsappairesponseservice.admin.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record PromotionCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 1800) String messageBody,
        @Size(max = 30) Set<@NotBlank @Size(min = 2, max = 80) String> aliases,
        Instant validFrom,
        Instant validUntil
) {
}
