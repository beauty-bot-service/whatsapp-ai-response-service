# Configuracion Completa

Guia operativa de todo lo configurado para este proyecto:

- OpenAI
- Meta Developers / WhatsApp Cloud API
- Google Calendar con service account
- Variables de entorno para local y deploy
- Pruebas de punta a punta

## 1) Requisitos previos

- JDK 21
- Maven 3.9+
- Cuenta de OpenAI
- Cuenta de Meta Developers (`developers.facebook.com`)
- Cuenta de Google (Gmail) para Google Cloud y Google Calendar (`calendar.google.com`)

### Verificar JDK 21 en Windows

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -v
```

`mvn -v` debe mostrar Java 21.

## 2) OpenAI

1. Entrar a OpenAI Platform y crear API key.
2. Configurar variable:

```powershell
$env:OPENAI_API_KEY="sk-..."
```

Variables relacionadas:

- `BEAUTY_BOT_AI_ENABLED`
- `BEAUTY_BOT_AI_DECISION_ENABLED`
- `BEAUTY_BOT_AI_DECISION_FALLBACK_ENABLED`
- `BEAUTY_BOT_AI_DECISION_PROMPT_ID`
- `BEAUTY_BOT_AI_REPLY_PROMPT_ID`

### 2.1 Prompt templates (mandar reglas una sola vez)

Si queres evitar enviar `instructions` en cada request, usa Prompt Templates de OpenAI.

1. Crear prompt template para decision conversacional.
2. Crear prompt template para redaccion final.
   - Referencias listas para copiar:
     - [docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md](prompts/AI_DECISION_PROMPT_TEMPLATE.md)
     - [docs/prompts/AI_REPLY_PROMPT_TEMPLATE.md](prompts/AI_REPLY_PROMPT_TEMPLATE.md)
3. Configurar IDs en variables de entorno:

```powershell
$env:BEAUTY_BOT_AI_DECISION_PROMPT_ID="pmpt_xxx"
$env:BEAUTY_BOT_AI_REPLY_PROMPT_ID="pmpt_yyy"
```

Opcional version fija:

```powershell
$env:BEAUTY_BOT_AI_DECISION_PROMPT_VERSION="1"
$env:BEAUTY_BOT_AI_REPLY_PROMPT_VERSION="1"
```

Variables que el backend inyecta en cada request:

- Prompt de decision: `{{conversation_context}}`
- Prompt de reply: `{{reply_context}}`

Opcional para cache extendida:

```powershell
$env:BEAUTY_BOT_AI_PROMPT_CACHE_RETENTION="24h"
```

Si no seteas estos IDs, el backend mantiene el comportamiento actual de fallback y manda `instructions` inline.

Con prompt template configurado, el payload de decision se reduce a contexto dinamico (mensaje actual, historial, estado de sesion, disponibilidad).

## 3) Meta Developers / WhatsApp Cloud API

Referencia principal: `https://developers.facebook.com/`

### 3.1 Crear y preparar app

1. Crear app en Meta Developers (tipo Business).
2. Agregar producto **WhatsApp**.
3. En **WhatsApp > API Setup** tomar:
   - `Phone Number ID`
   - token de acceso (idealmente permanente para produccion)
4. En **App Settings > Basic** tomar:
   - `App Secret`

### 3.2 Webhook

1. Exponer tu backend local con HTTPS (ejemplo: `ngrok http 8080`).
2. En Meta configurar callback URL:
   - `https://<tu-url-publica>/whatsapp-ai-response-service/v1/whatsapp/webhook`
3. Definir un verify token propio y usar el mismo en Meta y en el backend.
4. Suscribirse al campo `messages` del webhook.

### 3.3 Variables de entorno Meta/WhatsApp

```powershell
$env:WHATSAPP_ENABLED="true"
$env:WHATSAPP_VERIFY_TOKEN="tu_verify_token"
$env:WHATSAPP_ACCESS_TOKEN="tu_access_token"
$env:WHATSAPP_APP_SECRET="tu_app_secret"
$env:WHATSAPP_PHONE_NUMBER_ID="tu_phone_number_id"
$env:WHATSAPP_BASE_URL="https://graph.facebook.com/v25.0"
```

## 4) Google Calendar (Gmail + Google Cloud)

Referencias:

- `https://console.cloud.google.com/`
- `https://calendar.google.com/`

### 4.1 Habilitar API y crear service account

1. Entrar a Google Cloud con tu cuenta Gmail.
2. Crear o elegir proyecto.
3. Habilitar **Google Calendar API**.
4. Crear **Service Account**.
5. Generar clave JSON y descargar archivo (`service-account.json`).
6. Del JSON tomar `client_email` (termina en `iam.gserviceaccount.com`).

### 4.2 Compartir calendario

1. Entrar a Google Calendar.
2. Elegir el calendario a consultar.
3. Ir a **Settings and sharing**.
4. En **Share with specific people** agregar el `client_email` de la service account con permiso de lectura.
5. En **Integrate calendar** copiar el `Calendar ID`.

Notas:

- Si es calendario principal, muchas veces el ID coincide con un correo Gmail.
- Si no compartis el calendario con la service account, la app no vera disponibilidad real.

### 4.3 Variables de entorno Google Calendar

```powershell
$env:BEAUTY_BOT_CAN_CHECK_AVAILABILITY="true"
$env:BEAUTY_BOT_CALENDAR_ENABLED="true"
$env:BEAUTY_BOT_CALENDAR_TIME_ZONE="America/Argentina/Buenos_Aires"
$env:BEAUTY_BOT_CALENDAR_LOOKAHEAD_DAYS="14"
$env:BEAUTY_BOT_CALENDAR_SLOT_DURATION_MINUTES="30"
$env:BEAUTY_BOT_CALENDAR_MINIMUM_NOTICE_MINUTES="120"
$env:BEAUTY_BOT_CALENDAR_MAX_SUGGESTIONS="3"
$env:BEAUTY_BOT_CALENDAR_WORKING_DAYS="MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
$env:GOOGLE_CALENDAR_ID="tu_calendar_id"
```

### 4.4 Cargar credencial JSON en Base64

Opcion recomendada: `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64`.

```powershell
$env:GOOGLE_SERVICE_ACCOUNT_JSON_BASE64=[Convert]::ToBase64String(
  [Text.Encoding]::UTF8.GetBytes((Get-Content "C:\ruta\service-account.json" -Raw))
)
```

Alternativa:

- `GOOGLE_SERVICE_ACCOUNT_JSON` (JSON plano multilinea).

## 5) Mapeo de propiedades usadas por la app

La app usa `beauty-bot.*` en `application.yml`.

Grupos principales:

- `beauty-bot.ai.*`
- `beauty-bot.whatsapp.*`
- `beauty-bot.calendar.*`
- `beauty-bot.bot-capabilities.*`

Revisar ejemplo completo en `.env.example`.

## 6) Prueba local end-to-end

### 6.1 Levantar app

```powershell
mvn spring-boot:run
```

Base URL local:

`http://localhost:8080/whatsapp-ai-response-service/v1`

### 6.2 Probar chat interno (sin webhook)

Disponibilidad general:

```bash
curl --location "http://localhost:8080/whatsapp-ai-response-service/v1/chat/test" \
--header "Content-Type: application/json" \
--data "{\"phoneNumber\":\"5491123456789\",\"message\":\"Hola, tienen disponibilidad esta semana?\"}"
```

Fecha puntual (formato Argentina `dd/MM`):

```bash
curl --location "http://localhost:8080/whatsapp-ai-response-service/v1/chat/test" \
--header "Content-Type: application/json" \
--data "{\"phoneNumber\":\"5491123456789\",\"message\":\"Tienen turno el 5/4?\"}"
```

Fecha + hora puntual:

```bash
curl --location "http://localhost:8080/whatsapp-ai-response-service/v1/chat/test" \
--header "Content-Type: application/json" \
--data "{\"phoneNumber\":\"5491123456789\",\"message\":\"Tienen turno el 5/4 a las 15:30?\"}"
```

## 7) Troubleshooting rapido

### Error WhatsApp 401 code 190 (Authentication Error)

Revisar:

- `WHATSAPP_ACCESS_TOKEN` vigente
- `WHATSAPP_PHONE_NUMBER_ID` correcto
- app/numero de WhatsApp asociados al mismo negocio
- permisos del token para `whatsapp_business_messaging`

### No devuelve disponibilidad de Google Calendar

Revisar:

- `GOOGLE_CALENDAR_ID`
- calendario compartido con `client_email` de la service account
- `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` valido
- `BEAUTY_BOT_CALENDAR_ENABLED=true`
- `BEAUTY_BOT_CAN_CHECK_AVAILABILITY=true`

### Maven toma Java 8

En la misma terminal:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -v
```

## 8) Seguridad

- No subir claves reales a Git:
  - OpenAI API key
  - Meta access token
  - Meta app secret
  - JSON de service account
- Si alguna credencial se expuso, rotarla en su proveedor (Meta, OpenAI, Google Cloud).
