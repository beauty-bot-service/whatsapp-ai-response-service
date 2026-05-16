package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WhatsAppWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    public boolean isValid(String rawPayload, String signatureHeader, String appSecret) {
        if (!hasText(appSecret)) {
            // Legacy compatibility: if app secret is not configured, signature validation is skipped.
            return true;
        }

        if (rawPayload == null || !hasText(signatureHeader)) {
            return false;
        }

        String normalizedHeader = signatureHeader.trim();
        if (!normalizedHeader.regionMatches(true, 0, SIGNATURE_PREFIX, 0, SIGNATURE_PREFIX.length())) {
            return false;
        }

        String signatureHex = normalizedHeader.substring(SIGNATURE_PREFIX.length()).trim();
        byte[] receivedSignature = decodeHex(signatureHex);
        if (receivedSignature == null) {
            return false;
        }

        byte[] expectedSignature = computeHmacSha256(rawPayload, appSecret);
        return MessageDigest.isEqual(expectedSignature, receivedSignature);
    }

    private byte[] computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AppException(ResponseCode.WHATSAPP_SIGNATURE_VALIDATION_ERROR, e);
        }
    }

    private byte[] decodeHex(String hex) {
        if (!hasText(hex) || (hex.length() % 2) != 0) {
            return null;
        }

        int length = hex.length();
        byte[] output = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            output[i / 2] = (byte) ((high << 4) + low);
        }
        return output;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
