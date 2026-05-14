package com.beautybot.whatsappairesponseservice.persistence.mapper;

import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;
import com.beautybot.whatsappairesponseservice.persistence.entity.OutboundMessageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutboundMessageEntityMapper {
    OutboundMessage toModel(OutboundMessageEntity entity);
    OutboundMessageEntity toEntity(OutboundMessage model);
}
