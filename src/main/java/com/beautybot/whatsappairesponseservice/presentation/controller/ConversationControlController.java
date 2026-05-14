package com.beautybot.whatsappairesponseservice.presentation.controller;

import com.beautybot.whatsappairesponseservice.application.support.ConversationControlService;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.presentation.dto.ConversationControlRequest;
import com.beautybot.whatsappairesponseservice.presentation.dto.ConversationControlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversations")
public class ConversationControlController {

    private final ConversationControlService conversationControlService;

    @PostMapping("/takeover")
    public ConversationControlResponse takeover(@RequestBody ConversationControlRequest request) {
        ConversationSession session = conversationControlService.takeoverByHuman(
                request == null ? null : request.getPhoneNumber(),
                request == null ? null : request.getMessage()
        );
        return toResponse(session);
    }

    @PostMapping("/release")
    public ConversationControlResponse release(@RequestBody ConversationControlRequest request) {
        ConversationSession session = conversationControlService.releaseToBot(
                request == null ? null : request.getPhoneNumber()
        );
        return toResponse(session);
    }

    private ConversationControlResponse toResponse(ConversationSession session) {
        return ConversationControlResponse.builder()
                .sessionId(session.getId())
                .phoneNumber(session.getPhoneNumber())
                .state(session.getState())
                .requiresHuman(session.getRequiresHuman())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
