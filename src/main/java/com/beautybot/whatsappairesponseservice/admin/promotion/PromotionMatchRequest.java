package com.beautybot.whatsappairesponseservice.admin.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromotionMatchRequest(@NotBlank @Size(max = 2000) String message) {
}
