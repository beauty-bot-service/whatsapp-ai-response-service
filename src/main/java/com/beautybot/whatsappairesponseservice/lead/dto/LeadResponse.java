package com.beautybot.whatsappairesponseservice.lead.dto;

import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {
    private Long id;
    private Long clinicId;
    private Long conversationSessionId;
    private String phoneNumber;
    private String customerName;
    private String treatmentInterest;
    private Boolean firstTime;
    private String preferredTime;
    private LeadSource source;
    private LeadStatus status;
    private LeadTemperature temperature;
    private Integer score;
    private Long assignedToUserId;
    private LocalDateTime lastCustomerMessageAt;
    private LocalDateTime lastBotMessageAt;
    private LocalDateTime lastHumanMessageAt;
    private LocalDateTime readyForHumanAt;
    private LocalDateTime appointmentRequestedAt;
    private LocalDateTime appointmentBookedAt;
    private LocalDateTime lostAt;
    private LocalDateTime closedAt;
    private String notes;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
