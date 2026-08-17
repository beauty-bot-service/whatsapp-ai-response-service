package com.beautybot.whatsappairesponseservice.ai;

import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedMessageAnalyzer implements MessageAnalyzer {

    private static final Pattern SELF_IDENTIFICATION_PATTERN = Pattern.compile(
            "(?i)\\b(?:soy|me llamo|mi nombre es)\\s+([\\p{L} ]{2,60})"
    );
    private static final Pattern LEADING_NAME_WITH_CONTEXT_PATTERN = Pattern.compile(
            "(?i)^\\s*([\\p{L} ]{2,60})[\\.,]\\s*(?:me\\s+interesa(?:n)?|quiero|busco|consulta|consulto|queria|me\\s+gustaria)\\b"
    );
    private static final Pattern NAME_TRAILING_CONTEXT_PATTERN = Pattern.compile(
            "(?i)\\b(?:y|quiero|consulta|consulto|turno|precio|costo|valor|horario|ubicacion|direccion|donde|porque|pero|para|me\\s+interesa)\\b"
    );
    private static final Pattern STRICT_NAME_PATTERN = Pattern.compile(
            "^[\\p{L}]{2,40}(?:\\s+[\\p{L}]{2,40}){0,2}$"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?\\b");

    private static final List<String> MEDIA_REQUEST_TERMS = List.of(
            "foto", "fotos", "imagen", "imagenes", "video", "videos", "multimedia",
            "trabajos", "resultados", "antes y despues", "ejemplos"
    );

    private static final List<String> ADMINISTRATIVE_HUMAN_TERMS = List.of(
            "sena", "senar", "comprobante", "transferencia", "pago", "pague", "abone",
            "cbu", "alias", "mercado pago", "confirmo", "confirmar turno", "confirmacion",
            "te confirmo", "debo senar", "como confirmo"
    );

    private static final List<String> HUMAN_TERMS = List.of(
            "humano", "persona", "asesora", "asesor", "recepcionista", "alguien", "hablar con"
    );

    private static final List<String> ANGRY_TERMS = List.of(
            "reclamo", "queja", "malisimo", "pesimo", "nadie responde", "hace horas", "me canse"
    );

    private static final List<String> APPOINTMENT_TERMS = List.of(
            "turno", "turnito", "consulta", "reservar", "reserva", "agendar", "agenda",
            "cita", "coordinar", "quiero reservar", "me gustaria agendar"
    );

    private static final List<String> PRICE_TERMS = List.of(
            "precio", "precios", "promo", "promos", "promocion", "promociones",
            "descuento", "sale", "cuanto", "valor", "costo", "tarifa", "cuesta",
            "efectivo", "cuotas", "tarjeta", "$"
    );

    private static final List<String> LOCATION_TERMS = List.of(
            "ubicacion", "direccion", "donde estan", "sucursal", "local", "sede",
            "rio cuarto", "cordoba capital", "cerca", "kempes", "barrio"
    );

    private static final List<String> OPENING_HOURS_TERMS = List.of(
            "horario", "horarios", "abren", "cierran", "atiende", "atienden",
            "doctora", "doctor", "profesional"
    );

    private static final List<String> AVAILABILITY_TERMS = List.of(
            "disponibilidad", "disponible", "horarios disponibles", "turnos disponibles",
            "agenda disponible", "huecos", "huecos disponibles", "que turnos tienen", "lugares disponibles",
            "para cuando", "cuando tendrias", "cuando tenes", "cuando tienen", "hay turno",
            "tenes turno", "tendrias turno", "fecha", "fechas", "primer turnito"
    );
    private static final List<String> DATE_AVAILABILITY_TERMS = List.of(
            "turno", "turnos", "disponib", "agenda", "hueco", "huecos", "lugar", "libre"
    );

    private static final List<String> TREATMENT_INFO_TERMS = List.of(
            "que es", "de que se trata", "como funciona", "para que sirve", "en que consiste",
            "jeringa", "media jeringa", "jeringa completa", "completa", "marca", "marcas",
            "dura", "demora", "tiempo demora", "cuanto tiempo", "incluye", "retoque",
            "control", "procedimiento"
    );

    private static final List<String> CANCEL_TERMS = List.of("cancelar", "cancelo", "anular", "baja del turno");
    private static final List<String> RESCHEDULE_TERMS = List.of(
            "reprogramar", "cambiar turno", "mover turno", "pasar el turno", "otro horario",
            "otra fecha", "no puedo ir", "no llego"
    );
    private static final List<String> FIRST_TIME_YES_TERMS = List.of("si", "sip", "sisi", "claro", "correcto", "afirmativo");
    private static final List<String> FIRST_TIME_NO_TERMS = List.of("no", "nop", "negativo");
    private static final List<String> TIME_CONTEXT_TERMS = List.of(
            "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo",
            "manana", "tarde", "noche", "mediodia", "temprano", "semana que viene", "hoy", "pasado"
    );

    private static final List<String> CONTACT_ME_TERMS = List.of(
            "que me contacten", "me contacten", "que me escriban", "me escriban",
            "me pueden contactar", "me pueden escribir", "espero a que me contacten",
            "espero que me contacten", "cuando puedan", "cuando quieran", "me da igual",
            "cualquier horario", "sin preferencia", "no tengo preferencia", "no tengo horario",
            "que una asesora me contacte", "que una asesora me escriba"
    );

    private static final List<String> THANKS_TERMS = List.of(
            "gracias", "muchas gracias", "mil gracias", "dale gracias", "ok gracias"
    );

    @Override
    public MessageAnalysis analyze(String message, ConversationSession session) {
        String normalizedMessage = normalize(message);
        RequiredField waitingForField = session.getWaitingForField();
        boolean contactPreferenceReply = waitingForField == RequiredField.PREFERRED_TIME
                && containsAny(normalizedMessage, CONTACT_ME_TERMS);

        boolean medicalQuestion = MedicalQuestionClassifier.requiresHuman(message);
        boolean mediaRequest = containsAny(normalizedMessage, MEDIA_REQUEST_TERMS);
        boolean administrativeHuman = containsAny(normalizedMessage, ADMINISTRATIVE_HUMAN_TERMS);
        boolean wantsHuman = !contactPreferenceReply
                && (containsAny(normalizedMessage, HUMAN_TERMS) || mediaRequest || administrativeHuman);
        boolean angry = containsAny(normalizedMessage, ANGRY_TERMS);
        List<Intent> intents = resolveIntents(normalizedMessage, medicalQuestion, wantsHuman);
        Intent primaryIntent = intents.isEmpty() ? Intent.UNKNOWN : intents.getFirst();

        return MessageAnalysis.builder()
                .intent(primaryIntent)
                .intents(intents)
                .treatment(extractTreatment(normalizedMessage))
                .extractedName(extractName(message, normalizedMessage, session, waitingForField))
                .firstTime(extractFirstTime(normalizedMessage, waitingForField))
                .preferredTime(extractPreferredTime(message, normalizedMessage, waitingForField))
                .medicalQuestion(medicalQuestion)
                .wantsHuman(wantsHuman)
                .angryOrComplaint(angry)
                .rawMessage(message)
                .build();
    }

    private List<Intent> resolveIntents(String normalized, boolean medicalQuestion, boolean wantsHuman) {
        Set<Intent> detected = new LinkedHashSet<>();

        if (medicalQuestion) detected.add(Intent.MEDICAL_QUESTION);
        if (wantsHuman) detected.add(Intent.HUMAN_REQUEST);
        if (containsAny(normalized, ANGRY_TERMS)) detected.add(Intent.COMPLAINT);
        if (containsAny(normalized, RESCHEDULE_TERMS)) detected.add(Intent.RESCHEDULE);
        if (containsAny(normalized, CANCEL_TERMS)) detected.add(Intent.CANCEL);
        if (containsAny(normalized, PRICE_TERMS)) detected.add(Intent.PRICE_QUESTION);
        if (containsAny(normalized, TREATMENT_INFO_TERMS)) detected.add(Intent.TREATMENT_INFO);
        if (containsAny(normalized, LOCATION_TERMS)) detected.add(Intent.LOCATION_QUESTION);
        if (containsAny(normalized, OPENING_HOURS_TERMS)) detected.add(Intent.OPENING_HOURS_QUESTION);
        if (containsAny(normalized, AVAILABILITY_TERMS) || looksLikeDateAvailabilityQuestion(normalized)) {
            detected.add(Intent.AVAILABILITY_QUESTION);
        }
        if (containsAny(normalized, APPOINTMENT_TERMS)) detected.add(Intent.APPOINTMENT_REQUEST);
        if (isGreeting(normalized)) detected.add(Intent.GREETING);
        if (containsAny(normalized, THANKS_TERMS)) detected.add(Intent.THANKS);

        if (detected.isEmpty()) {
            detected.add(Intent.UNKNOWN);
        }

        return List.copyOf(detected);
    }

    private String extractTreatment(String normalized) {
        Set<String> treatments = new LinkedHashSet<>();
        if (containsAny(normalized, List.of("botox", "toxina", "dysport", "bruxismo"))) {
            treatments.add("Botox");
        }
        if (containsAny(normalized, List.of("rinolips"))) {
            treatments.add("Rinolips");
        }
        if (containsAny(normalized, List.of("relleno", "acido hialuronico", "hialuronico", "a.h", "a h", "labios", "lips"))
                || looksLikeAcidoHialuronicoAbbreviation(normalized)) {
            treatments.add("Relleno con acido hialuronico");
        }
        if (containsAny(normalized, List.of("hilos", "hilo tensor", "hilos tensores"))) {
            treatments.add("Hilos tensores");
        }
        if (containsAny(normalized, List.of("sculptra", "bioestimulador", "bioestimuladores"))) {
            treatments.add("Bioestimulador");
        }
        if (containsAny(normalized, List.of("hydroxyfill"))) {
            treatments.add("Hydroxyfill");
        }
        if (containsAny(normalized, List.of("rinoplastia", "nariz"))) {
            treatments.add("Rinoplastia");
        }
        if (containsAny(normalized, List.of("lipo", "liposuccion", "lipoescultura"))) {
            treatments.add("Lipoescultura");
        }
        if (containsAny(normalized, List.of("aumento mamario", "mamas", "pechos", "implantes"))) {
            treatments.add("Aumento mamario");
        }
        if (containsAny(normalized, List.of("depilacion", "laser"))) {
            treatments.add("Depilacion laser");
        }
        if (containsAny(normalized, List.of("limpieza facial", "facial"))) {
            treatments.add("Limpieza facial");
        }
        if (containsAny(normalized, List.of("peeling"))) {
            treatments.add("Peeling");
        }
        if (treatments.isEmpty()) {
            return null;
        }
        return String.join(" y ", treatments);
    }

    private boolean looksLikeAcidoHialuronicoAbbreviation(String normalized) {
        return containsAnyWholeWord(normalized, List.of("ah"))
                && containsAny(normalized, List.of("botox", "labios", "relleno", "promo", "promos", "hialuronico"));
    }

    private String extractName(
            String original,
            String normalized,
            ConversationSession session,
            RequiredField waitingForField
    ) {
        if (session.getCustomerName() != null) {
            return null;
        }

        String extractedFromSelfIdentification = extractNameFromSelfIdentification(original);
        if (extractedFromSelfIdentification != null) {
            if (waitingForField == RequiredField.NAME || isStrictNameCandidate(extractedFromSelfIdentification)) {
                return extractedFromSelfIdentification;
            }
        }

        String extractedFromLeadingContext = extractNameFromLeadingContext(original);
        if (extractedFromLeadingContext != null) {
            return extractedFromLeadingContext;
        }

        if (waitingForField == RequiredField.NAME && !containsStrongTimeSignal(normalized)) {
            String relaxedCandidate = trimTrailingContextFromName(cleanName(original));
            if (isLikelyNameCandidate(relaxedCandidate)) {
                return relaxedCandidate;
            }
        }

        return null;
    }

    private Boolean extractFirstTime(String normalized, RequiredField waitingForField) {
        if (waitingForField == RequiredField.FIRST_TIME) {
            if (containsAny(normalized, List.of(
                    "primera vez", "primera consulta", "es mi primera", "nunca fui",
                    "nunca realice", "nunca me realice", "nunca hice", "nunca me hice"
            ))
                    || containsAnyWholeWord(normalized, FIRST_TIME_YES_TERMS)) {
                return true;
            }
            if (containsAny(normalized, List.of(
                    "ya fui", "ya soy paciente", "no es primera vez", "ya realice",
                    "ya me realice", "ya hice", "ya me hice"
            ))
                    || containsAnyWholeWord(normalized, FIRST_TIME_NO_TERMS)) {
                return false;
            }
            return null;
        }

        if (containsAny(normalized, List.of(
                "primera vez", "primera consulta", "es mi primera", "nunca fui",
                "nunca realice", "nunca me realice", "nunca hice", "nunca me hice"
        ))
                || containsAnyWholeWord(normalized, List.of("nuevo", "nueva"))) {
            return true;
        }
        if (containsAny(normalized, List.of(
                "ya fui", "ya soy paciente", "no es primera vez", "ya realice",
                "ya me realice", "ya hice", "ya me hice"
        ))
                || containsAnyWholeWord(normalized, List.of("paciente"))) {
            return false;
        }
        return null;
    }

    private String extractPreferredTime(String original, String normalized, RequiredField waitingForField) {
        if (containsStrongTimeSignal(normalized)) {
            return original.trim();
        }
        if (waitingForField == RequiredField.PREFERRED_TIME) {
            if (containsAny(normalized, CONTACT_ME_TERMS)) {
                return "Prefiere que una asesora lo contacte";
            }
            if (isLikelyTimeReplyWhileWaiting(normalized)) {
                return original.trim();
            }
        }
        return null;
    }

    private boolean containsStrongTimeSignal(String normalized) {
        if (containsAny(normalized, TIME_CONTEXT_TERMS)) {
            return true;
        }
        if (containsDateToken(normalized)) {
            return true;
        }
        if (Pattern.compile("\\b\\d{1,2}:\\d{2}\\b").matcher(normalized).find()) {
            return true;
        }
        if (Pattern.compile("\\b(?:a las|desde las|tipo|entre las|despues de las|antes de las|a partir de las)\\s+\\d{1,2}\\b").matcher(normalized).find()) {
            return true;
        }
        if (Pattern.compile("\\b(?:primer|primerito|ultimo)\\s+turn").matcher(normalized).find()) {
            return true;
        }
        return Pattern.compile("\\b\\d{1,2}\\s*(?:am|pm|hs)\\b").matcher(normalized).find();
    }

    private boolean looksLikeDateAvailabilityQuestion(String normalized) {
        return containsDateToken(normalized) && containsAny(normalized, DATE_AVAILABILITY_TERMS);
    }

    private boolean containsDateToken(String normalized) {
        return DATE_PATTERN.matcher(normalized).find();
    }

    private boolean isLikelyTimeReplyWhileWaiting(String normalized) {
        String compact = normalized.trim();
        if (Pattern.compile("^\\d{1,2}$").matcher(compact).matches()) {
            return true;
        }
        if (Pattern.compile("^\\d{1,2}\\s*(?:hs|am|pm)$").matcher(compact).matches()) {
            return true;
        }
        return containsAny(compact, List.of("manana", "tarde", "noche", "mediodia"));
    }

    private boolean isLikelyNameCandidate(String value) {
        String cleaned = cleanName(value);
        if (cleaned.split(" ").length > 4) {
            return false;
        }
        if (containsAny(normalize(cleaned), APPOINTMENT_TERMS)
                || containsAny(normalize(cleaned), PRICE_TERMS)
                || containsAny(normalize(cleaned), LOCATION_TERMS)
                || containsAny(normalize(cleaned), OPENING_HOURS_TERMS)
                || isDisallowedNameCandidate(cleaned)) {
            return false;
        }
        return Pattern.compile("^[\\p{L} ]{2,40}$").matcher(cleaned).matches();
    }

    private String extractNameFromSelfIdentification(String original) {
        Matcher matcher = SELF_IDENTIFICATION_PATTERN.matcher(original);
        if (!matcher.find()) {
            return null;
        }

        String candidate = cleanName(matcher.group(1));
        candidate = trimTrailingContextFromName(candidate);
        if (candidate.isBlank()) {
            return null;
        }
        return candidate;
    }

    private String extractNameFromLeadingContext(String original) {
        Matcher matcher = LEADING_NAME_WITH_CONTEXT_PATTERN.matcher(original);
        if (!matcher.find()) {
            return null;
        }

        String candidate = cleanName(matcher.group(1));
        if (isLikelyNameCandidate(candidate)) {
            return candidate;
        }
        return null;
    }

    private String trimTrailingContextFromName(String candidate) {
        Matcher matcher = NAME_TRAILING_CONTEXT_PATTERN.matcher(candidate);
        if (!matcher.find()) {
            return candidate;
        }
        return candidate.substring(0, matcher.start()).trim();
    }

    private boolean isStrictNameCandidate(String value) {
        return STRICT_NAME_PATTERN.matcher(value).matches() && !isDisallowedNameCandidate(value);
    }

    private boolean isDisallowedNameCandidate(String value) {
        String normalized = normalize(value);
        return List.of(
                "hola", "buen dia", "buenas", "buenas tardes", "buenas noches",
                "dale", "ok", "si", "no", "gracias", "perfecto", "genial",
                "excelente", "espectacular"
        ).contains(normalized);
    }

    private boolean containsAny(String text, List<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    private boolean containsAnyWholeWord(String text, List<String> terms) {
        return terms.stream().anyMatch(term -> Pattern.compile("\\b" + Pattern.quote(term) + "\\b").matcher(text).find());
    }

    private boolean isGreeting(String normalized) {
        return Pattern.compile("^(hola|buen dia|buenos dias|buenas|buenas tardes|buenas noches|como va)\\b").matcher(normalized).find();
    }

    private String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private String cleanName(String name) {
        return name.trim()
                .replaceAll("[.,;:!?]", "")
                .replaceAll("\\s+", " ");
    }
}
