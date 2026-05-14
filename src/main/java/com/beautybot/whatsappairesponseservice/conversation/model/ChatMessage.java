package com.beautybot.whatsappairesponseservice.conversation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String phoneNumber;
    private String message;
    private String channel;
    private String externalMessageId;
}



