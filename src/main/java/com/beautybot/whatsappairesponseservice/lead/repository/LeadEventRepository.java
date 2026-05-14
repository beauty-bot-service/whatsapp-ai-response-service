package com.beautybot.whatsappairesponseservice.lead.repository;

import com.beautybot.whatsappairesponseservice.lead.entity.LeadEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadEventRepository extends JpaRepository<LeadEventEntity, Long> {
    List<LeadEventEntity> findByClinicIdAndLeadIdOrderByCreatedAtDesc(Long clinicId, Long leadId);
}
