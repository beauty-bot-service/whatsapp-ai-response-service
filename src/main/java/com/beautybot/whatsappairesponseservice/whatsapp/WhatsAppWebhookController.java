package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final BeautyBotProperties properties;
    private final WhatsAppWebhookService webhookService;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        BeautyBotProperties.Whatsapp whatsapp = properties.getWhatsapp();
        if (!whatsapp.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not enabled");
        }

        boolean validMode = "subscribe".equals(mode);
        boolean validToken = whatsapp.getVerifyToken() != null && whatsapp.getVerifyToken().equals(verifyToken);
        if (validMode && validToken && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        if (!webhookService.isValidSignature(rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        webhookService.processWebhookAsync(rawPayload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}


