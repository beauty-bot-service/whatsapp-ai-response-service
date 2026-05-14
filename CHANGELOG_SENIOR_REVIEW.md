# Cambios aplicados por review senior

## Seguridad y configuración
- Se removieron secretos hardcodeados del `application.yml`.
- Se movió la configuración de WhatsApp bajo `beauty-bot.whatsapp`, que es el prefijo que lee `BeautyBotProperties`.
- Se agregaron flags por variables de entorno para AI, Calendar, WhatsApp, outbound y endpoints de test.
- Los endpoints `/chat/test`, `/leads` y `/whatsapp/test/send` quedan deshabilitados por defecto con `beauty-bot.test-endpoints-enabled=false`.

## Webhook y envío saliente
- El webhook de WhatsApp ahora responde `EVENT_RECEIVED` y procesa el payload de forma async mediante `@Async`.
- Se agregó outbox persistida para mensajes salientes: `OUTBOUND_MESSAGES`.
- El webhook ya no llama directamente al cliente de WhatsApp; encola la respuesta y el servicio de outbox intenta despacharla.
- El outbox registra estado `PENDING`, `SENT` o `FAILED`, cantidad de intentos, último error y timestamp de envío.

## Transacciones y flujo conversacional
- `HandleIncomingMessageUseCase` ya no mantiene una transacción abierta durante todo el flujo.
- Se separó el guardado inbound de la decisión conversacional, evitando que llamadas a OpenAI/Calendar queden dentro de una transacción larga.
- La aplicación de decisión, guardado outbound y creación de lead se hacen en una transacción corta.

## Validación y robustez
- `InboundMessageNormalizer` ahora tolera request nulo.
- `ChatMessageValidator` valida teléfono, mensaje, channel y externalMessageId con límites razonables.
- `ConversationDecisionValidator` normaliza y limita longitudes de reply, summary y datos extraídos por IA.
- Se agregó lock pesimista al lookup de sesión reutilizable para reducir condiciones de carrera por conversación.

## Persistencia
- Se reemplazó `@Data` por `@Getter`/`@Setter` en entidades JPA.
- Se agregaron índices más útiles para sesiones por teléfono y fecha.
- Se agregaron migraciones SQL iniciales en `src/main/resources/db/migration`.
- `ddl-auto` quedó en `validate` por defecto para evitar cambios automáticos de schema fuera de local.

## HTTP clients
- Se centralizó la creación de `RestClient` en `RestClientFactory`, con timeouts configurados.

## Pendientes recomendados
- Agregar dependencia Flyway/Liquibase en el `pom.xml` o `build.gradle` si todavía no existe.
- Agregar autenticación real con Spring Security para endpoints administrativos si se habilitan fuera de local.
- Agregar scheduler/worker para retry periódico de outbox si se quiere retry automático sin intervención manual.
- Agregar tests de integración con PostgreSQL/Testcontainers.

## Refactor conversacional final
- Se eliminó el contrato legacy `BotDecision`; el contrato único del pipeline ahora es `ConversationDecision`.
- `ConversationService.applyDecision` y `HumanHandoffService` trabajan directamente con `ConversationDecision`.
- Se eliminó `ConversationOrchestrator`, porque el fallback rule-based ahora decide directamente dentro de `RuleBasedConversationDecisionService`.
- Se eliminaron clases legacy que ya no formaban parte del pipeline principal: `ConversationEngine`, `ReplyGenerator`, `OpenAiReplyService` y `OpenAiReplyRequestFactory`.
- `BotResponseService` quedó como fachada liviana para construir decisiones rule-based, delegando textos a factories especializadas.
- Se dividió la generación de respuestas en `HandoffReplyFactory`, `LeadCollectionReplyFactory`, `InformationalReplyFactory`, `AvailabilityReplyFactory`, `HumanSummaryFactory` y `ReplyStyleNormalizer`.
- `ConversationContextBuilder` quedó reducido a orquestar providers, delegando en `ClinicContextFactory`, `BotCapabilitiesFactory`, `RecentMessageContextProvider`, `AvailabilityContextProvider` y `DecisionRulesProvider`.
- Se removieron tests legacy asociados a clases eliminadas y se actualizaron los tests que todavía dependían de `BotDecision`.

## Tercera tanda: hardening de pre-produccion

Cambios agregados:

- `pom.xml` actualizado con:
  - `spring-boot-starter-security`
  - `spring-boot-starter-actuator`
  - `flyway-core`
  - `flyway-database-postgresql`
  - `micrometer-registry-prometheus`
  - Testcontainers para PostgreSQL/JUnit.
- Flyway habilitado por defecto en `application.yml`.
- Seguridad por API key para endpoints internos:
  - `/chat/test`
  - `/leads`
  - `/whatsapp/test/**`
  - `/actuator/**`
- Worker real de outbox:
  - reclama mensajes `PENDING`
  - marca `DISPATCHING`
  - envía por WhatsApp
  - marca `SENT`, `PENDING` con backoff o `FAILED`
  - libera locks stale de dispatch.
- Outbox endurecida con:
  - `NEXT_ATTEMPT_AT`
  - `LOCKED_AT`
  - status `DISPATCHING`
  - retry backoff configurable.
- Idempotencia reforzada:
  - precheck por `channel + externalMessageId`
  - constraint path sigue cortando duplicados por carrera
  - métricas para duplicados.
- Concurrencia por conversación:
  - lock en memoria por `phoneNumber` durante procesamiento webhook por instancia
  - lock DB por `phoneNumber` dentro de la transacción de inbound/session creation.
- Observabilidad:
  - métricas Micrometer para inbound, outbound, AI, fallback, handoff, outbox y servicios externos
  - health indicators para OpenAI config y WhatsApp config
  - phone masking para logs.
- Rate limits/errores externos:
  - clasificador para timeout/connectivity, 429, 401, 403, 5xx y otros errores HTTP
  - `WhatsAppSendResult` ampliado con resultados diferenciados.
- Tests agregados:
  - clasificador de errores externos
  - comportamiento base de outbox/retry.

Notas:

- No pude ejecutar Maven en este entorno porque no está instalado el comando `mvn`.
- Antes de mergear, correr localmente `mvn clean test` y ajustar cualquier detalle de compilación/imports.
- Para producción multi-instancia, el procesamiento ordenado ideal sigue siendo una cola/event table por conversación. La versión actual mejora mucho el orden y reduce carreras, pero no reemplaza una cola distribuida dedicada.
