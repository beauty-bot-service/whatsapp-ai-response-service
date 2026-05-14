package com.beautybot.whatsappairesponseservice.persistence.repository;

import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;
import com.beautybot.whatsappairesponseservice.persistence.dao.OutboundMessageDao;
import com.beautybot.whatsappairesponseservice.persistence.mapper.OutboundMessageEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboundMessageModelRepository {

    private final OutboundMessageDao dao;
    private final OutboundMessageEntityMapper mapper;

    public OutboundMessage save(OutboundMessage message) {
        return mapper.toModel(dao.save(mapper.toEntity(message)));
    }
}
