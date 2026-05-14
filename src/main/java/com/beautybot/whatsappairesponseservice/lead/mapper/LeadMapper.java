package com.beautybot.whatsappairesponseservice.lead.mapper;

import com.beautybot.whatsappairesponseservice.lead.dto.LeadResponse;
import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeadMapper {
    LeadResponse toResponse(LeadEntity entity);
}
