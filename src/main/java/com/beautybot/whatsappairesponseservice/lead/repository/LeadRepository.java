package com.beautybot.whatsappairesponseservice.lead.repository;

import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<LeadEntity, Long> {

    Optional<LeadEntity> findByClinicIdAndPhoneNumber(Long clinicId, String phoneNumber);

    Optional<LeadEntity> findByClinicIdAndConversationSessionId(Long clinicId, Long conversationSessionId);

    Optional<LeadEntity> findByClinicIdAndId(Long clinicId, Long id);

    Page<LeadEntity> findByClinicId(Long clinicId, Pageable pageable);

    Page<LeadEntity> findByClinicIdAndStatus(Long clinicId, LeadStatus status, Pageable pageable);

    Page<LeadEntity> findByClinicIdAndTemperature(Long clinicId, LeadTemperature temperature, Pageable pageable);

    Page<LeadEntity> findByClinicIdAndTreatmentInterestIgnoreCase(Long clinicId, String treatmentInterest, Pageable pageable);
}
