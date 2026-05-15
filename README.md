# beauty-bot-mvp

MVP de bot de WhatsApp para una clinica estetica.

Objetivo del MVP:

- Responder rapido por WhatsApp.
- Captar intencion.
- Pedir datos basicos.
- Dejar encaminado el turno hasta que responda una persona.

## Stack

- Java 21
- Spring Boot 3.3.5
- PostgreSQL (deploy) / H2 en memoria (fallback local)
- Decision conversacional por IA (opcional) con fallback rule-based
- Consulta de disponibilidad con Google Calendar (opcional)
- Dockerfile multi-stage
- MapStruct para mapeos entre capas

## Dependencia `beauty-bot-common` desde GitHub Packages

Este servicio consume:

```xml
<dependency>
  <groupId>com.beautybot</groupId>
  <artifactId>beauty-bot-common</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

En `pom.xml` tambien esta configurado el repositorio Maven de GitHub Packages:

```xml
<repositories>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/TU_USUARIO/TU_REPO</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

### Configuracion local (`~/.m2/settings.xml`)

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>TU_USUARIO_GITHUB</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Scopes del token: `read:packages`, `write:packages` y `repo` si el repositorio es privado.

## Build en Railway con Dockerfile (GitHub Packages privado)

Si el package es privado, el build necesita credenciales en tiempo de build.

1. Crear variables en Railway:
   - `GITHUB_USERNAME`
   - `GITHUB_TOKEN`
2. Pasarlas como build args para Docker:
   - `GITHUB_USERNAME=${{GITHUB_USERNAME}}`
   - `GITHUB_TOKEN=${{GITHUB_TOKEN}}`

El `Dockerfile` ya fue adaptado para usar esas variables y generar `/root/.m2/settings.xml` durante el build.

## Documentacion de configuracion

- Guia completa paso a paso: [docs/CONFIGURACION_COMPLETA.md](docs/CONFIGURACION_COMPLETA.md)
- Variables de entorno de referencia: [.env.example](.env.example)
- Templates sugeridos de IA:
  - [docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md](docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md)
  - [docs/prompts/AI_REPLY_PROMPT_TEMPLATE.md](docs/prompts/AI_REPLY_PROMPT_TEMPLATE.md)

## Arquitectura en capas

```text
controller (DTO)
  -> service (MODEL)
     -> repository (MODEL <-> ENTITY con MapStruct)
        -> dao (JPA ENTITY)
```

## Flujo tecnico

```text
POST /chat/test
  -> ChatController (DTO request/response)
  -> ChatService (MODEL)
  -> ConversationService.getOrCreate(phoneNumber)
  -> saveInbound
  -> ConversationContextBuilder
  -> ConversationDecisionRouter
       -> AI decision (opcional, si beauty-bot.ai.enabled=true y beauty-bot.ai.decision.enabled=true)
       -> fallback rule-based (si falla AI y beauty-bot.ai.decision.fallback-enabled=true)
  -> ConversationDecisionValidator (normaliza y corrige invariantes)
  -> ConversationService.applyDecision
  -> saveOutbound
  -> LeadService si ya estan los datos minimos
```

## Como correr

Requisito: ejecutar Maven con JDK 21 (`mvn -v` debe mostrar `Java version: 21`).

```bash
mvn spring-boot:run
```

La app levanta en:

```text
http://localhost:8080/whatsapp-ai-response-service/v1
```

Para forzar PostgreSQL local:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/beautybot"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
```

## Probar sin IA

Por defecto, la IA viene apagada:

```yaml
beauty-bot:
  ai:
    enabled: false
```

Esto usa respuestas base del backend y analisis por reglas, sin consumir tokens.

Request:

```http
POST http://localhost:8080/whatsapp-ai-response-service/v1/chat/test
Content-Type: application/json
```

```json
{
  "phoneNumber": "5491123456789",
  "message": "Hola, soy Florencia, quiero turno para botox el viernes a la tarde"
}
```

## Activar IA

Setear variable de entorno:

```bash
export OPENAI_API_KEY="tu_api_key"
```

En Windows PowerShell:

```powershell
$env:OPENAI_API_KEY="tu_api_key"
```

Y cambiar config:

```yaml
beauty-bot:
  ai:
    enabled: true
    base-url: "https://api.openai.com/v1"
    api-key: ${OPENAI_API_KEY:}
    model: "gpt-5.4-mini"
```

Opcional (recomendado): usar Prompt Templates para no mandar instrucciones en cada request:

```text
BEAUTY_BOT_AI_DECISION_PROMPT_ID=pmpt_xxx
BEAUTY_BOT_AI_REPLY_PROMPT_ID=pmpt_yyy
```

Con eso, el backend envia solo contexto dinamico como variables (`conversation_context` y `reply_context`).

Tambien podes arrancar con property:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--beauty-bot.ai.enabled=true"
```

## Que hace la IA en este MVP

La IA puede decidir el estado conversacional y la respuesta final usando contexto completo.
Si falla o devuelve algo invalido, el backend aplica validaciones e invariantes y puede caer al flujo rule-based.

## Endpoints

### Chat test

```http
POST /whatsapp-ai-response-service/v1/chat/test
```

### Ver leads

```http
GET /whatsapp-ai-response-service/v1/leads
```

### WhatsApp webhook (Meta Cloud API)

```http
GET  /whatsapp-ai-response-service/v1/whatsapp/webhook
POST /whatsapp-ai-response-service/v1/whatsapp/webhook
```

### WhatsApp test send (saliente manual)

```http
POST /whatsapp-ai-response-service/v1/whatsapp/test/send
```

## Disponibilidad con Google Calendar

El bot puede consultar disponibilidad real y sugerir proximos slots cuando:

- `beauty-bot.bot-capabilities.can-check-availability=true`
- `beauty-bot.calendar.enabled=true`
- hay credenciales validas de Google Calendar

Variables de entorno recomendadas:

```text
BEAUTY_BOT_CAN_CHECK_AVAILABILITY=true
BEAUTY_BOT_CALENDAR_ENABLED=true
BEAUTY_BOT_CALENDAR_TIME_ZONE=America/Argentina/Buenos_Aires
BEAUTY_BOT_CALENDAR_LOOKAHEAD_DAYS=14
BEAUTY_BOT_CALENDAR_SLOT_DURATION_MINUTES=30
BEAUTY_BOT_CALENDAR_MINIMUM_NOTICE_MINUTES=120
BEAUTY_BOT_CALENDAR_MAX_SUGGESTIONS=3
BEAUTY_BOT_CALENDAR_WORKING_START=09:00
BEAUTY_BOT_CALENDAR_WORKING_END=18:00
BEAUTY_BOT_CALENDAR_WORKING_DAYS=MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
GOOGLE_CALENDAR_ID=<calendar_id>
GOOGLE_SERVICE_ACCOUNT_JSON_BASE64=<service_account_json_en_base64>
```

Notas:

1. Crear una service account en Google Cloud y habilitar Google Calendar API.
2. Compartir el calendario con el email de la service account con permiso de lectura.
3. Usar `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` (recomendado) para evitar problemas de formato multiline.
4. Esta version sugiere disponibilidad, pero no confirma ni crea turnos en agenda automaticamente.

## Integracion con WhatsApp Cloud API

Configurar variables de entorno:

```powershell
$env:OPENAI_API_KEY="tu_api_key"
$env:WHATSAPP_VERIFY_TOKEN="token_verificacion_webhook"
$env:WHATSAPP_ACCESS_TOKEN="token_de_acceso_meta"
$env:WHATSAPP_APP_SECRET="app_secret_meta"
$env:WHATSAPP_PHONE_NUMBER_ID="phone_number_id_meta"
```

Config minima en `application.yml`:

```yaml
beauty-bot:
  whatsapp:
    enabled: true
    verify-token: ${WHATSAPP_VERIFY_TOKEN:}
    access-token: ${WHATSAPP_ACCESS_TOKEN:}
    app-secret: ${WHATSAPP_APP_SECRET:}
    phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID:}
    base-url: "https://graph.facebook.com/v22.0"
```

Luego:

1. Exponer local con un tunel HTTPS (ej. `ngrok http 8080`).
2. En Meta Developers, setear callback URL: `https://<tu-url-publica>/whatsapp-ai-response-service/v1/whatsapp/webhook`.
3. Usar el mismo `verify-token` para la verificacion.
4. Suscribirse al evento `messages` del webhook.
5. Configurar `WHATSAPP_APP_SECRET` para validar `X-Hub-Signature-256`.

Con eso, los mensajes entrantes de WhatsApp se procesan por `ChatService` y la respuesta se envia por Cloud API.

Para validar credenciales de salida antes del webhook, podes enviar un mensaje manual:

```http
POST http://localhost:8080/whatsapp-ai-response-service/v1/whatsapp/test/send
Content-Type: application/json
```

```json
{
  "toPhoneNumber": "54911XXXXXXXX",
  "message": "Prueba de integracion WhatsApp desde el backend"
}
```

## Primer Deploy (Railway o Render)

### 1) Build con Docker

Este repo ya incluye `Dockerfile` y `.dockerignore`.

### 2) Base de datos

Crear un PostgreSQL administrado (Railway Postgres o Render Postgres).

### 3) Variables de entorno minimas

```text
PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_JPA_HIBERNATE_DDL_AUTO=update

BEAUTY_BOT_AI_ENABLED=true|false
OPENAI_API_KEY=<tu_key_si_ai_activa>
```

Opcionales para WhatsApp:

```text
WHATSAPP_VERIFY_TOKEN=...
WHATSAPP_ACCESS_TOKEN=...
WHATSAPP_APP_SECRET=...
WHATSAPP_PHONE_NUMBER_ID=...
BEAUTY_BOT_CAN_CHECK_AVAILABILITY=true|false
BEAUTY_BOT_CALENDAR_ENABLED=true|false
GOOGLE_CALENDAR_ID=...
GOOGLE_SERVICE_ACCOUNT_JSON_BASE64=...
```

### 4) Railway (resumen)

1. `New Project` -> `Deploy from GitHub Repo`.
2. Agregar servicio PostgreSQL.
3. Configurar variables de entorno del servicio web.
4. Deploy; health check con `GET /whatsapp-ai-response-service/v1/leads`.

### 5) Render (resumen)

1. `New +` -> `Web Service` desde GitHub (Docker).
2. Crear PostgreSQL en Render.
3. Copiar URL JDBC/user/pass a variables del web service.
4. Deploy; probar `POST /whatsapp-ai-response-service/v1/chat/test`.

## Clases principales

```text
ai/
  MessageAnalyzer.java
  RuleBasedMessageAnalyzer.java

application/decision/
  ConversationDecisionRouter.java
  AiConversationDecisionService.java
  RuleBasedConversationDecisionService.java
  ConversationDecisionValidator.java
  ConversationContextBuilder.java

orchestrator/
  ConversationOrchestrator.java

resolver/
  MissingDataResolver.java

policy/
  HandoffPolicy.java

reply/
  BotResponseService.java

service/
  ChatService.java
  ConversationService.java
  LeadService.java

mapper/
  *Mapper.java

entity/
  *Entity.java

repository/
  *ModelRepository.java

dao/
  *Dao.java
```

## Nota importante

Para este MVP, el bot no agenda automaticamente. Solo deja el lead listo para que una asesora confirme disponibilidad.
