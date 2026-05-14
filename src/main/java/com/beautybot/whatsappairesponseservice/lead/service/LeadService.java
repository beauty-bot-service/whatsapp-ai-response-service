package com.beautybot.whatsappairesponseservice.lead.service;

import com.beautybot.whatsappairesponseservice.lead.dto.LeadResponse;
import com.beautybot.whatsappairesponseservice.lead.dto.LeadUpsertRequest;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {
    LeadResponse upsertFromConversation(LeadUpsertRequest request);
    LeadResponse getById(Long clinicId, Long leadId);
    LeadResponse getByPhoneNumber(Long clinicId, String phoneNumber);
    Page<LeadResponse> search(Long clinicId, LeadStatus status, LeadTemperature temperature, String treatmentInterest, Pageable pageable);
    LeadResponse changeStatus(Long clinicId, Long leadId, LeadStatus newStatus, String changedBy);
    LeadResponse assign(Long clinicId, Long leadId, Long assignedToUserId, String assignedBy);
    LeadResponse addNote(Long clinicId, Long leadId, String note, String createdBy);
}
