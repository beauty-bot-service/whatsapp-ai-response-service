package com.beautybot.whatsappairesponseservice.conversation.model;

import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class MessageAnalysis {

    private Intent intent;
    @Builder.Default
    private List<Intent> intents = List.of();
    private String treatment;
    private String extractedName;
    private Boolean firstTime;
    private String preferredTime;
    private Boolean medicalQuestion;
    private Boolean wantsHuman;
    private Boolean angryOrComplaint;
    private String rawMessage;

    public List<Intent> intentsOrPrimary() {
        if (intents != null && !intents.isEmpty()) {
            return intents;
        }
        if (intent == null) {
            return List.of(Intent.UNKNOWN);
        }
        return List.of(intent);
    }

    public boolean hasIntent(Intent value) {
        return intentsOrPrimary().contains(value);
    }

    public List<Intent> safeIntents() {
        return intents == null ? Collections.emptyList() : intents;
    }
}


