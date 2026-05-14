package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InboundMessageNormalizerTest {

    private final InboundMessageNormalizer normalizer = new InboundMessageNormalizer();

    @Test
    void normalizesPhoneChannelAndExternalId() {
        ChatMessage raw = ChatMessage.builder()
                .phoneNumber("+54 9 11 1234-5678")
                .message("  hola  ")
                .channel(" whatsapp ")
                .externalMessageId(" abc-123 ")
                .build();

        ChatMessage normalized = normalizer.normalize(raw);

        assertThat(normalized.getPhoneNumber()).isEqualTo("5491112345678");
        assertThat(normalized.getMessage()).isEqualTo("hola");
        assertThat(normalized.getChannel()).isEqualTo("WHATSAPP");
        assertThat(normalized.getExternalMessageId()).isEqualTo("abc-123");
    }

    @Test
    void defaultsChannelToApiWhenMissing() {
        ChatMessage raw = ChatMessage.builder()
                .phoneNumber(" 11 5555 4444 ")
                .message("consulta")
                .channel("   ")
                .build();

        ChatMessage normalized = normalizer.normalize(raw);

        assertThat(normalized.getPhoneNumber()).isEqualTo("1155554444");
        assertThat(normalized.getChannel()).isEqualTo("API");
        assertThat(normalized.getExternalMessageId()).isNull();
    }
}
