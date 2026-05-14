package com.beautybot.whatsappairesponseservice.persistence.mapper;

import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationMessageEntity;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMessageEntityMapper {

    @Mapping(target = "session", ignore = true)
    ConversationMessageEntity toEntity(ConversationMessage model);

    ConversationMessage toModel(ConversationMessageEntity entity);
}


