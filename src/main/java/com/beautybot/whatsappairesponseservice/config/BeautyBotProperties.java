package com.beautybot.whatsappairesponseservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "beauty-bot")
public class BeautyBotProperties {

    private Long clinicId = 1L;
    private String clinicName;
    private String location;
    private String openingHours;
    private String attendingDoctor = "la doctora asignada";
    private boolean advisorNotificationEnabled;
    private String advisorNotificationPhoneNumber;
    /**
     * Habilita endpoints de prueba/administracion simples. Debe quedar false fuera de local.
     */
    private boolean testEndpointsEnabled = false;
    /**
     * Propiedad legacy.
     * Si se informa y no hay overrides por estado, se usa para todos los estados reutilizables.
     */
    private Integer conversationSessionReuseHours;
    private Conversation conversation = new Conversation();
    private BotCapabilitiesConfig botCapabilities = new BotCapabilitiesConfig();
    private CalendarConfig calendar = new CalendarConfig();
    private Ai ai = new Ai();
    private Whatsapp whatsapp = new Whatsapp();
    private Security security = new Security();
    private Observability observability = new Observability();

    @Data
    public static class Conversation {
        /**
         * Ventana de reutilizacion para COLLECTING_DATA.
         */
        private Integer collectingReuseHours;

        /**
         * Ventana de reutilizacion para READY_FOR_HUMAN.
         */
        private Integer readyForHumanReuseHours;

        /**
         * Ventana de reutilizacion para HUMAN_HANDOFF.
         */
        private Integer humanHandoffReuseHours;
    }

    @Data
    public static class BotCapabilitiesConfig {
        private boolean canCollectLeadData = true;
        private boolean canNotifyHuman = true;
        private boolean canConfirmAppointment = false;
        private boolean canCheckAvailability = false;
        private boolean canGiveExactPrices = false;
        private boolean canProvideMedicalAdvice = false;
        private boolean canCancelAppointments = false;
        private boolean canRescheduleAppointments = false;
    }

    @Data
    public static class CalendarConfig {
        /**
         * Habilita consulta de disponibilidad real en calendario.
         */
        private boolean enabled = false;

        /**
         * Zona horaria para calcular y formatear slots.
         */
        private String timeZone = "America/Argentina/Buenos_Aires";

        /**
         * Cuantos dias hacia adelante buscar.
         */
        private int lookaheadDays = 14;

        /**
         * Duracion de cada slot sugerido.
         */
        private int slotDurationMinutes = 30;

        /**
         * Anticipacion minima desde "ahora" para ofrecer el primer slot.
         */
        private int minimumNoticeMinutes = 120;

        /**
         * Maximo de slots a sugerir en cada respuesta.
         */
        private int maxSuggestions = 3;

        private WorkingHours workingHours = new WorkingHours();
        private Google google = new Google();

        @Data
        public static class WorkingHours {
            /**
             * Formato HH:mm.
             */
            private String start = "09:00";

            /**
             * Formato HH:mm.
             */
            private String end = "18:00";

            /**
             * Dias habilitados para sugerencias.
             */
            private List<String> days = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        }

        @Data
        public static class Google {
            /**
             * ID del calendario de Google Calendar (ej: primary o correo del calendario).
             */
            private String calendarId;

            /**
             * Service account JSON en Base64 para evitar multiline en variables.
             */
            private String serviceAccountJsonBase64;

            /**
             * Alternativa al Base64: JSON plano (solo para entornos donde sea seguro).
             */
            private String serviceAccountJson;
        }
    }

    @Data
    public static class Ai {
        /**
         * Habilita llamadas a OpenAI. El uso concreto se controla con decision.enabled.
         */
        private boolean enabled = false;

        private Decision decision = new Decision();

        /**
         * Base URL compatible con OpenAI Responses API.
         */
        private String baseUrl = "https://api.openai.com/v1";

        /**
         * API key. Recomendado: cargar desde variable de entorno OPENAI_API_KEY.
         */
        private String apiKey;

        /**
         * Modelo usado para redactar la respuesta final.
         */
        private String model = "gpt-5.4-mini";

        /**
         * Timeout para llamadas a IA.
         */
        private int timeoutSeconds = 12;

        /**
         * Politica de retencion de prompt cache.
         * Valores esperados: in_memory o 24h.
         */
        private String promptCacheRetention;

        @Data
        public static class Decision {
            /**
             * Si esta en true, OpenAI decide estado, datos y respuesta.
             */
            private boolean enabled = false;

            /**
             * Si falla OpenAI, usa el flujo rule-based local.
             */
            private boolean fallbackEnabled = true;
        }

    }

    @Data
    public static class Security {
        /**
         * Habilita filtro por API key en endpoints internos/actuator.
         */
        private boolean internalApiKeyEnabled = true;

        /**
         * Header esperado para endpoints internos.
         */
        private String internalApiKeyHeader = "Authorization";

        /**
         * Valor esperado. Recomendado: Bearer <token>.
         */
        private String internalApiKey;
    }

    @Data
    public static class Observability {
        /**
         * Enmascara phoneNumber en logs. Debe quedar true en produccion.
         */
        private boolean maskPhoneNumbers = true;
    }

    @Data
    public static class Whatsapp {
        /**
         * Habilita el adaptador de webhook + envio a WhatsApp Cloud API.
         */
        private boolean enabled = false;

        /**
         * Registra payloads completos para diagnostico. Puede contener datos personales y debe permanecer apagado normalmente.
         */
        private boolean logPayloads = false;

        /**
         * Token para la verificacion del webhook (hub.verify_token).
         */
        private String verifyToken;

        /**
         * Token de acceso del numero de WhatsApp Cloud API.
         */
        private String accessToken;

        /**
         * App Secret de Meta para validar la firma X-Hub-Signature-256 del webhook.
         */
        private String appSecret;

        /**
         * Phone Number ID de Meta usado para enviar mensajes.
         */
        private String phoneNumberId;

        /**
         * Base URL del Graph API incluyendo version.
         * Ejemplo: https://graph.facebook.com/v22.0
         */
        private String baseUrl = "https://graph.facebook.com/v22.0";
    }
}
