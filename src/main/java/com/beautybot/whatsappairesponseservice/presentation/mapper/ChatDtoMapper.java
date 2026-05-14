package com.beautybot.whatsappairesponseservice.presentation.mapper;

import com.beautybot.whatsappairesponseservice.presentation.dto.ChatRequest;
import com.beautybot.whatsappairesponseservice.presentation.dto.ChatResponse;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatDtoMapper {

    ChatMessage toModel(ChatRequest request);

    ChatResponse toDto(ChatResult result);
}



