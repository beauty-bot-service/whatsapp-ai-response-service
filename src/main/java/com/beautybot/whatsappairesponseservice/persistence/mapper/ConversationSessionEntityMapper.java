package com.beautybot.whatsappairesponseservice.persistence.mapper;

import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationSessionEntity;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationSessionEntityMapper {

    ConversationSession toModel(ConversationSessionEntity entity);

    ConversationSessionEntity toEntity(ConversationSession model);
}



