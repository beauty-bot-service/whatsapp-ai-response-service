package com.beautybot.whatsappairesponseservice.ai;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class MedicalQuestionClassifier {

    private static final List<String> ESCALATION_TERMS = List.of(
            "me conviene", "que me recomendas", "que me recomiendas", "riesgo", "riesgos",
            "diagnostico", "dolor", "embarazada", "embarazo", "medicacion", "enfermedad",
            "apto", "apta", "hematoma", "alergia", "alergias", "contraindicacion",
            "contraindicaciones", "post tratamiento", "postratamiento", "complicacion",
            "complicaciones", "efecto adverso", "efectos adversos", "sintoma", "sintomas",
            "dosis", "cuidados", "resultado garantizado", "resultados garantizados"
    );

    private MedicalQuestionClassifier() {
    }

    public static boolean requiresHuman(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = Normalizer.normalize(message.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ESCALATION_TERMS.stream().anyMatch(normalized::contains);
    }
}
