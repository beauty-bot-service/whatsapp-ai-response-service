package com.beautybot.whatsappairesponseservice.whatsapp;

import jakarta.validation.constraints.NotBlank;

public record WhatsAppSendTestRequest(
        @NotBlank String toPhoneNumber,
        @NotBlank String message
) {
}
