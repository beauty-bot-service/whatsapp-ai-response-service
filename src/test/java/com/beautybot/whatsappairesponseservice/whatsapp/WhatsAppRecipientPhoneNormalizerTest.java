package com.beautybot.whatsappairesponseservice.whatsapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppRecipientPhoneNormalizerTest {

    private final WhatsAppRecipientPhoneNormalizer normalizer = new WhatsAppRecipientPhoneNormalizer();

    @Test
    void removesArgentinaMobilePrefixForMetaRecipient() {
        assertThat(normalizer.normalize("5491166536556")).isEqualTo("541166536556");
    }

    @Test
    void leavesOtherPhoneNumbersUnchanged() {
        assertThat(normalizer.normalize("59171234567")).isEqualTo("59171234567");
    }

    @Test
    void doesNotAlterUnexpectedArgentinaNumberLength() {
        assertThat(normalizer.normalize("549123")).isEqualTo("549123");
    }
}
