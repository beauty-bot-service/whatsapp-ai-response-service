package com.beautybot.whatsappairesponseservice.presentation.dto;

import lombok.Data;

@Data
public class ConversationControlRequest {
    private String phoneNumber;
    private String message;
}
