package com.beautybot.whatsappairesponseservice.lead.service.impl;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.lead.dto.LeadResponse;
import com.beautybot.whatsappairesponseservice.lead.dto.LeadUpsertRequest;
import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.entity.LeadEventEntity;
import com.beautybot.whatsappairesponseservice.lead.mapper.LeadMapper;
import com.beautybot.whatsappairesponseservice.lead.model.LeadEventType;
import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import com.beautybot.whatsappairesponseservice.lead.repository.LeadEventRepository;
import com.beautybot.whatsappairesponseservice.lead.repository.LeadRepository;
import com.beautybot.whatsappairesponseservice.lead.service.LeadScoringService;
import com.beautybot.whatsappairesponseservice.lead.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadEventRepository leadEventRepository;
    private final LeadMapper leadMapper;
    private final LeadScoringService leadScoringService;

    @Override
    public LeadResponse upsertFromConversation(LeadUpsertRequest request) {
        validateUpsertRequest(request);
        Long clinicId = request.getClinicId();
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        LocalDateTime now = LocalDateTime.now();

        LeadEntity lead = leadRepository.findByClinicIdAndPhoneNumber(clinicId, phoneNumber).orElse(null);
        boolean created = lead == null;
        if (created) {
            lead = LeadEntity.builder()
                    .clinicId(clinicId)
                    .conversationSessionId(request.getConversationSessionId())
                    .phoneNumber(phoneNumber)
                    .source(request.getSource() != null ? request.getSource() : LeadSource.WHATSAPP_INBOUND)
                    .status(LeadStatus.NEW)
                    .temperature(LeadTemperature.COLD)
                    .score(0)
                    .build();
        }

        LeadStatus previousStatus = lead.getStatus();
        LeadTemperature previousTemperature = lead.getTemperature();

        mergeFromRequest(lead, request);
        lead.setLastCustomerMessageAt(now);
        LeadStatus nextStatus = resolveStatus(request, lead);
        lead.setStatus(nextStatus);
        applyStatusTimestamps(lead, now);

        int score = leadScoringService.calculateScore(lead);
        LeadTemperature temperature = leadScoringService.calculateTemperature(score);
        lead.setScore(score);
        lead.setTemperature(temperature);

        LeadEntity saved = leadRepository.save(lead);

        if (created) {
            saveEvent(saved, LeadEventType.CREATED, null, saved.getStatus(), "Lead created from conversational flow.", "system");
        } else if (hasRelevantDataChange(request)) {
            saveEvent(saved, LeadEventType.UPDATED, previousStatus, saved.getStatus(), "Lead updated from conversational flow.", "system");
        }

        if (!Objects.equals(previousStatus, saved.getStatus())) {
            saveEvent(saved, LeadEventType.STATUS_CHANGED, previousStatus, saved.getStatus(), "Lead status changed by conversational flow.", "system");
        }
        if (!Objects.equals(previousTemperature, saved.getTemperature())) {
            saveEvent(saved, LeadEventType.TEMPERATURE_CHANGED, null, null, "Lead temperature changed to " + saved.getTemperature(), "system");
        }

        return leadMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getById(Long clinicId, Long leadId) {
        validateClinicId(clinicId);
        LeadEntity lead = leadRepository.findByClinicIdAndId(clinicId, leadId)
                .orElseThrow(() -> new AppException(ResponseCode.LEAD_NOT_FOUND));
        return leadMapper.toResponse(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getByPhoneNumber(Long clinicId, String phoneNumber) {
        validateClinicId(clinicId);
        String normalized = normalizePhoneNumber(phoneNumber);
        LeadEntity lead = leadRepository.findByClinicIdAndPhoneNumber(clinicId, normalized)
                .orElseThrow(() -> new AppException(ResponseCode.LEAD_NOT_FOUND));
        return leadMapper.toResponse(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> search(
            Long clinicId,
            LeadStatus status,
            LeadTemperature temperature,
            String treatmentInterest,
            Pageable pageable
    ) {
        validateClinicId(clinicId);
        Page<LeadEntity> result;
        if (status != null) {
            result = leadRepository.findByClinicIdAndStatus(clinicId, status, pageable);
        } else if (temperature != null) {
            result = leadRepository.findByClinicIdAndTemperature(clinicId, temperature, pageable);
        } else if (hasText(treatmentInterest)) {
            result = leadRepository.findByClinicIdAndTreatmentInterestIgnoreCase(clinicId, treatmentInterest.trim(), pageable);
        } else {
            result = leadRepository.findByClinicId(clinicId, pageable);
        }
        return result.map(leadMapper::toResponse);
    }

    @Override
    public LeadResponse changeStatus(Long clinicId, Long leadId, LeadStatus newStatus, String changedBy) {
        validateClinicId(clinicId);
        if (newStatus == null) {
            throw new AppException(ResponseCode.LEAD_STATUS_REQUIRED);
        }

        LeadEntity lead = leadRepository.findByClinicIdAndId(clinicId, leadId)
                .orElseThrow(() -> new AppException(ResponseCode.LEAD_NOT_FOUND));
        LeadStatus previousStatus = lead.getStatus();
        LeadTemperature previousTemperature = lead.getTemperature();

        lead.setStatus(newStatus);
        applyStatusTimestamps(lead, LocalDateTime.now());
        int score = leadScoringService.calculateScore(lead);
        lead.setScore(score);
        lead.setTemperature(leadScoringService.calculateTemperature(score));
        LeadEntity saved = leadRepository.save(lead);

        if (!Objects.equals(previousStatus, saved.getStatus())) {
            saveEvent(saved, LeadEventType.STATUS_CHANGED, previousStatus, saved.getStatus(), "Manual status change.", changedBy);
        }
        if (!Objects.equals(previousTemperature, saved.getTemperature())) {
            saveEvent(saved, LeadEventType.TEMPERATURE_CHANGED, null, null, "Lead temperature changed to " + saved.getTemperature(), changedBy);
        }
        return leadMapper.toResponse(saved);
    }

    @Override
    public LeadResponse assign(Long clinicId, Long leadId, Long assignedToUserId, String assignedBy) {
        validateClinicId(clinicId);
        LeadEntity lead = leadRepository.findByClinicIdAndId(clinicId, leadId)
                .orElseThrow(() -> new AppException(ResponseCode.LEAD_NOT_FOUND));
        lead.setAssignedToUserId(assignedToUserId);
        LeadEntity saved = leadRepository.save(lead);
        saveEvent(saved, LeadEventType.ASSIGNED, null, null, "Assigned to user " + assignedToUserId, assignedBy);
        return leadMapper.toResponse(saved);
    }

    @Override
    public LeadResponse addNote(Long clinicId, Long leadId, String note, String createdBy) {
        validateClinicId(clinicId);
        if (!hasText(note)) {
            throw new AppException(ResponseCode.LEAD_NOTE_REQUIRED);
        }
        LeadEntity lead = leadRepository.findByClinicIdAndId(clinicId, leadId)
                .orElseThrow(() -> new AppException(ResponseCode.LEAD_NOT_FOUND));
        String newNote = note.trim();
        if (hasText(lead.getNotes())) {
            lead.setNotes(lead.getNotes() + System.lineSeparator() + newNote);
        } else {
            lead.setNotes(newNote);
        }
        LeadEntity saved = leadRepository.save(lead);
        saveEvent(saved, LeadEventType.NOTE_ADDED, null, null, "Note added.", createdBy);
        return leadMapper.toResponse(saved);
    }

    private void validateUpsertRequest(LeadUpsertRequest request) {
        if (request == null) {
            throw new AppException(ResponseCode.LEAD_REQUEST_REQUIRED);
        }
        validateClinicId(request.getClinicId());
        if (!hasText(request.getPhoneNumber())) {
            throw new AppException(ResponseCode.LEAD_PHONE_REQUIRED);
        }
    }

    private void validateClinicId(Long clinicId) {
        if (clinicId == null) {
            throw new AppException(ResponseCode.LEAD_CLINIC_ID_REQUIRED);
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.trim().replace(" ", "");
    }

    private void mergeFromRequest(LeadEntity lead, LeadUpsertRequest request) {
        if (hasText(request.getCustomerName())) {
            lead.setCustomerName(request.getCustomerName().trim());
        }
        if (hasText(request.getTreatmentInterest())) {
            lead.setTreatmentInterest(request.getTreatmentInterest().trim());
        }
        if (request.getFirstTime() != null) {
            lead.setFirstTime(request.getFirstTime());
        }
        if (hasText(request.getPreferredTime())) {
            lead.setPreferredTime(request.getPreferredTime().trim());
        }
        if (hasText(request.getMetadata())) {
            lead.setMetadata(request.getMetadata().trim());
        }
        if (lead.getConversationSessionId() == null && request.getConversationSessionId() != null) {
            lead.setConversationSessionId(request.getConversationSessionId());
        }
        if (lead.getSource() == null || lead.getSource() == LeadSource.UNKNOWN) {
            lead.setSource(request.getSource() != null ? request.getSource() : LeadSource.WHATSAPP_INBOUND);
        }
    }

    private LeadStatus resolveStatus(LeadUpsertRequest request, LeadEntity lead) {
        if (request.getSuggestedStatus() != null) {
            return request.getSuggestedStatus();
        }
        if (Boolean.TRUE.equals(request.getAppointmentRequested())) {
            return LeadStatus.APPOINTMENT_REQUESTED;
        }
        if (Boolean.TRUE.equals(request.getReadyForHuman())) {
            return LeadStatus.READY_FOR_HUMAN;
        }
        if (lead.getStatus() == LeadStatus.NEW && hasCommercialData(lead)) {
            return LeadStatus.QUALIFYING;
        }
        return lead.getStatus();
    }

    private boolean hasCommercialData(LeadEntity lead) {
        return hasText(lead.getCustomerName())
                || hasText(lead.getTreatmentInterest())
                || lead.getFirstTime() != null
                || hasText(lead.getPreferredTime());
    }

    private void applyStatusTimestamps(LeadEntity lead, LocalDateTime now) {
        if (lead.getStatus() == LeadStatus.READY_FOR_HUMAN && lead.getReadyForHumanAt() == null) {
            lead.setReadyForHumanAt(now);
        }
        if (lead.getStatus() == LeadStatus.APPOINTMENT_REQUESTED && lead.getAppointmentRequestedAt() == null) {
            lead.setAppointmentRequestedAt(now);
        }
        if (lead.getStatus() == LeadStatus.APPOINTMENT_BOOKED && lead.getAppointmentBookedAt() == null) {
            lead.setAppointmentBookedAt(now);
        }
        if (lead.getStatus() == LeadStatus.LOST && lead.getLostAt() == null) {
            lead.setLostAt(now);
        }
        if (lead.getStatus() == LeadStatus.CLOSED && lead.getClosedAt() == null) {
            lead.setClosedAt(now);
        }
    }

    private void saveEvent(
            LeadEntity lead,
            LeadEventType type,
            LeadStatus previousStatus,
            LeadStatus newStatus,
            String description,
            String createdBy
    ) {
        leadEventRepository.save(LeadEventEntity.builder()
                .leadId(lead.getId())
                .clinicId(lead.getClinicId())
                .eventType(type)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .createdBy(trimToNull(createdBy))
                .build());
    }

    private boolean hasRelevantDataChange(LeadUpsertRequest request) {
        return hasText(request.getCustomerName())
                || hasText(request.getTreatmentInterest())
                || request.getFirstTime() != null
                || hasText(request.getPreferredTime())
                || hasText(request.getMetadata())
                || request.getConversationSessionId() != null
                || request.getSuggestedStatus() != null
                || Boolean.TRUE.equals(request.getReadyForHuman())
                || Boolean.TRUE.equals(request.getAppointmentRequested());
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
