package com.beautybot.whatsappairesponseservice.lead.dto;

import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadUpsertRequest {
    private Long clinicId;
    private Long conversationSessionId;
    private String phoneNumber;
    private String customerName;
    private String treatmentInterest;
    private Boolean firstTime;
    private String preferredTime;
    private LeadSource source;
    private LeadStatus suggestedStatus;
    private Boolean readyForHuman;
    private Boolean appointmentRequested;
    private String metadata;
}
