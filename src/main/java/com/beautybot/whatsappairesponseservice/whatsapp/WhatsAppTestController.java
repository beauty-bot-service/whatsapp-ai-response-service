package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "beauty-bot.test-endpoints-enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/whatsapp/test")
public class WhatsAppTestController {

    private final BeautyBotProperties properties;
    private final WhatsAppCloudApiClient cloudApiClient;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendTestMessage(@Valid @RequestBody WhatsAppSendTestRequest request) {
        if (!properties.getWhatsapp().isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "error", "whatsapp.disabled"));
        }

        WhatsAppSendResult result = cloudApiClient.sendTextMessage(request.toPhoneNumber(), request.message());
        if (result == WhatsAppSendResult.SENT) {
            return ResponseEntity.ok(Map.of("ok", true, "result", result.name()));
        }

        HttpStatus status = switch (result) {
            case NOT_CONFIGURED -> HttpStatus.CONFLICT;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case DISABLED -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return ResponseEntity.status(status)
                .body(Map.of("ok", false, "result", result.name()));
    }
}
