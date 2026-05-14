package com.beautybot.whatsappairesponseservice.conversation.policy;

import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import org.springframework.stereotype.Component;

@Component
public class HandoffPolicy {

    public boolean shouldHandoffImmediately(MessageAnalysis analysis) {
        return Boolean.TRUE.equals(analysis.getMedicalQuestion())
                || Boolean.TRUE.equals(analysis.getWantsHuman())
                || Boolean.TRUE.equals(analysis.getAngryOrComplaint())
                || analysis.hasIntent(Intent.MEDICAL_QUESTION)
                || analysis.hasIntent(Intent.HUMAN_REQUEST)
                || analysis.hasIntent(Intent.RESCHEDULE)
                || analysis.hasIntent(Intent.CANCEL);
    }
}


