# Properties completas para Railway

| Dato | Valor |
|---|---|
| Version | `2.3.1` |
| Servicio web | Frontend React y backend Spring Boot en una sola imagen |
| Base de datos | PostgreSQL administrado por Railway |
| Migraciones | Flyway automatico hasta `V10` |
| Calendario | No se utiliza |

Este es el archivo de referencia para configurar el servicio `whatsapp-ai-response-service` en Railway. `.env.example` sigue siendo la plantilla tecnica para desarrollo local.

## Antes de pegar el bloque

1. El servicio PostgreSQL de los ejemplos debe llamarse `Postgres`. Si tiene otro nombre, reemplazarlo dentro de las referencias `${{Postgres.*}}`.
2. Reemplazar todos los valores `<...>` antes de desplegar. No dejar placeholders literales.
3. Cargar los secretos como variables selladas de Railway.
4. No definir `PORT`: Railway lo inyecta automaticamente.
5. No definir `VITE_API_BASE_URL`: el frontend usa el mismo dominio y ruta base que Spring Boot.

## Bloque RAW completo

```dotenv
# Build: acceso de solo lectura a GitHub Packages
GITHUB_USERNAME=<usuario-github>
GITHUB_TOKEN=<token-github-con-read-packages>

# PostgreSQL de Railway
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# Datos de la clinica
BEAUTY_BOT_CLINIC_ID=1
BEAUTY_BOT_CLINIC_NAME=Doctor Beauty
BEAUTY_BOT_LOCATION=Cochabamba 427
BEAUTY_BOT_OPENING_HOURS=Lunes a viernes de 9 a 18 hs
BEAUTY_BOT_ATTENDING_DOCTOR=Dra. Nombre Apellido

# Panel administrativo
BEAUTY_BOT_ADMIN_ENABLED=true
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=<email-administrador>
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=<clave-unica-de-al-menos-12-caracteres>
BEAUTY_BOT_ADMIN_SECURE_COOKIE=true

# Seguridad y observabilidad
BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=false
BEAUTY_BOT_INTERNAL_API_KEY_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_HEADER=Authorization
BEAUTY_BOT_INTERNAL_API_KEY=Bearer <secreto-interno-largo-y-aleatorio>
BEAUTY_BOT_MASK_PHONE_NUMBERS=true

# OpenAI
BEAUTY_BOT_AI_ENABLED=true
BEAUTY_BOT_AI_BASE_URL=https://api.openai.com/v1
BEAUTY_BOT_AI_DECISION_ENABLED=true
BEAUTY_BOT_AI_DECISION_FALLBACK_ENABLED=true
BEAUTY_BOT_AI_MODEL=gpt-5.4-mini
BEAUTY_BOT_AI_TIMEOUT_SECONDS=20
BEAUTY_BOT_AI_PROMPT_CACHE_RETENTION=24h
OPENAI_API_KEY=<openai-api-key>
BEAUTY_BOT_AI_DECISION_PROMPT_ID=<pmpt_id-publicado>
BEAUTY_BOT_AI_DECISION_PROMPT_VERSION=<version-publicada>

# Reutilizacion de conversaciones
BEAUTY_BOT_CONVERSATION_COLLECTING_REUSE_HOURS=24
BEAUTY_BOT_CONVERSATION_READY_FOR_HUMAN_REUSE_HOURS=168
BEAUTY_BOT_CONVERSATION_HUMAN_HANDOFF_REUSE_HOURS=168

# WhatsApp Cloud API
WHATSAPP_ENABLED=true
WHATSAPP_VERIFY_TOKEN=<verify-token-acordado>
WHATSAPP_ACCESS_TOKEN=<meta-access-token>
WHATSAPP_APP_SECRET=<meta-app-secret>
WHATSAPP_PHONE_NUMBER_ID=<meta-phone-number-id>
WHATSAPP_BASE_URL=https://graph.facebook.com/v25.0

# Aviso interno a la asesora
BEAUTY_BOT_ADVISOR_NOTIFICATION_ENABLED=true
BEAUTY_BOT_ADVISOR_NOTIFICATION_PHONE_NUMBER=<telefono-whatsapp-en-formato-internacional>
```

## Properties temporales del primer administrador

`BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL` y `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD` crean el usuario solamente si ese email todavia no existe. La contraseña debe tener al menos 12 caracteres.

Despues de verificar el primer login:

1. Eliminar ambas variables de Railway.
2. Redeployar el servicio.
3. El usuario continuara disponible porque queda persistido en `ADMIN_USERS` con la contraseña hasheada.

Cambiar `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD` no restablece la contraseña de un usuario existente.

## Activacion por etapas

Si todavia no estan disponibles las credenciales externas, usar temporalmente:

```dotenv
BEAUTY_BOT_AI_ENABLED=false
BEAUTY_BOT_AI_DECISION_ENABLED=false
WHATSAPP_ENABLED=false
BEAUTY_BOT_ADVISOR_NOTIFICATION_ENABLED=false
```

El panel de promociones y PostgreSQL pueden probarse con esas integraciones apagadas. Para probar los endpoints `/chat/test`, activar temporalmente `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=true` y volver a `false` al finalizar.

## Properties que no deben agregarse

Desde `2.3.0`, el flujo conversacional no consulta Google Calendar. No configurar:

```text
BEAUTY_BOT_CAN_CHECK_AVAILABILITY
BEAUTY_BOT_CALENDAR_ENABLED
BEAUTY_BOT_CALENDAR_*
GOOGLE_CALENDAR_ID
GOOGLE_SERVICE_ACCOUNT_JSON
GOOGLE_SERVICE_ACCOUNT_JSON_BASE64
```

Aunque existan propiedades legacy en el código, `can-check-availability` y `calendar.enabled` estan forzadas en `false`.

## Frontend y puerto

No existe un servicio frontend separado. El `Dockerfile` compila React y copia `admin-web/dist` dentro del JAR. Railway expone un unico servicio mediante HTTPS y Spring Boot escucha el `PORT` inyectado por la plataforma.

```text
Panel: https://<dominio-railway>/whatsapp-ai-response-service/v1/
API:   https://<dominio-railway>/whatsapp-ai-response-service/v1/api/**
```

## Tablas y migraciones

Con `SPRING_FLYWAY_ENABLED=true`, el backend crea automaticamente:

| Migracion | Tablas |
|---|---|
| `V8` | `PROMOTIONS`, `PROMOTION_ALIASES`, `PROMOTION_EVENTS` |
| `V9` | `ADMIN_USERS` |
| `V10` | `CONVERSATION_PROMOTION_DELIVERIES` |

No ejecutar manualmente esos scripts en una base administrada por Flyway. Los archivos fuente estan en `src/main/resources/db/migration/`.

## Verificacion posterior al deploy

1. Abrir `https://<dominio-railway>/whatsapp-ai-response-service/v1/`.
2. Iniciar sesion con el usuario bootstrap.
3. Confirmar en los logs que Flyway llego a `V10`.
4. Consultar `/actuator/health` usando el header configurado en `BEAUTY_BOT_INTERNAL_API_KEY_HEADER` y el valor exacto de `BEAUTY_BOT_INTERNAL_API_KEY`.
5. Consultar `/actuator/info` y verificar `build.version=2.3.1`.
