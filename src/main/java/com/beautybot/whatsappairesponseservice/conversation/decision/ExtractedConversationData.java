package com.beautybot.whatsappairesponseservice.conversation.decision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.beautybot.whatsappairesponseservice.conversation.state.ContactPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedConversationData {
    private String customerName;
    private String treatmentInterest;
    private Boolean firstTime;
    private String preferredTime;
    private ContactPreference contactPreference;
}
