package com.beautybot.whatsappairesponseservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String message;
}


