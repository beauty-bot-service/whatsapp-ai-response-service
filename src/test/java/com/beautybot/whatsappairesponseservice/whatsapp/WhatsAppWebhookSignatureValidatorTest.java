package com.beautybot.whatsappairesponseservice.whatsapp;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppWebhookSignatureValidatorTest {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final WhatsAppWebhookSignatureValidator validator = new WhatsAppWebhookSignatureValidator();

    @Test
    void rejectsWhenAppSecretIsNotConfigured() {
        assertThat(validator.isValid("{\"entry\":[]}", null, null)).isFalse();
        assertThat(validator.isValid("{\"entry\":[]}", "", "   ")).isFalse();
    }

    @Test
    void rejectsWhenSignatureHeaderIsMissingAndSecretIsConfigured() {
        assertThat(validator.isValid("{\"entry\":[]}", null, "secret")).isFalse();
    }

    @Test
    void validatesKnownGoodSignature() throws Exception {
        String payload = "{\"entry\":[]}";
        String secret = "top-secret";
        String signature = "sha256=" + hmacSha256Hex(payload, secret);

        assertThat(validator.isValid(payload, signature, secret)).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        assertThat(validator.isValid("{\"entry\":[]}", "sha256=deadbeef", "top-secret")).isFalse();
    }

    private String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
