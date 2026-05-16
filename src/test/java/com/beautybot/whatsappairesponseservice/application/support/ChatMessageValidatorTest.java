package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.application.exception.InvalidChatMessageException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageValidatorTest {

    private final ChatMessageValidator validator = new ChatMessageValidator();

    @Test
    void rejectsBlankPhoneNumberAfterNormalization() {
        ChatMessage normalized = ChatMessage.builder()
                .phoneNumber("")
                .message("hola")
                .channel("WHATSAPP")
                .externalMessageId("wamid.1")
                .build();

        assertThatThrownBy(() -> validator.validateNormalized(normalized))
                .isInstanceOfSatisfying(InvalidChatMessageException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.INVALID_PHONE_NUMBER));
    }

    @Test
    void acceptsValidNormalizedPhoneNumber() {
        ChatMessage normalized = ChatMessage.builder()
                .phoneNumber("5491112345678")
                .message("hola")
                .channel("WHATSAPP")
                .externalMessageId("wamid.1")
                .build();

        assertThatCode(() -> validator.validateNormalized(normalized))
                .doesNotThrowAnyException();
    }
}
