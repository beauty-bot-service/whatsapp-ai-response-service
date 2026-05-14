package com.beautybot.whatsappairesponseservice.persistence.dao;

import com.beautybot.whatsappairesponseservice.persistence.entity.OutboundMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundMessageDao extends JpaRepository<OutboundMessageEntity, Long> {
}
