package com.beautybot.whatsappairesponseservice.presentation.controller;

import com.beautybot.whatsappairesponseservice.presentation.dto.ChatRequest;
import com.beautybot.whatsappairesponseservice.presentation.dto.ChatResponse;
import com.beautybot.whatsappairesponseservice.presentation.mapper.ChatDtoMapper;
import com.beautybot.whatsappairesponseservice.application.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "beauty-bot.test-endpoints-enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatDtoMapper chatDtoMapper;

    @PostMapping("/test")
    public ChatResponse test(@Valid @RequestBody ChatRequest request) {
        return chatDtoMapper.toDto(chatService.handleMessage(chatDtoMapper.toModel(request)));
    }

}
