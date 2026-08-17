# Properties de promociones y administración

| Dato | Valor |
|---|---|
| Versión | `2.0.0` |
| Archivo backend | `src/main/resources/application.yml` |
| Plantilla de variables | `.env.example` |
| Archivo frontend | `admin-web/.env.example` |

Este documento separa las variables nuevas de las variables existentes que sólo son necesarias para probar el módulo.

## 1. Properties nuevas del backend

| Variable de entorno | Property Spring | Default | Obligatoria | Secreto |
|---|---|---:|---|---|
| `BEAUTY_BOT_CLINIC_ID` | `beauty-bot.clinic-id` | `1` | Sí en ambientes reales | No |
| `BEAUTY_BOT_ADMIN_ENABLED` | `beauty-bot.admin.enabled` | `true` | No | No |
| `BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL` | `beauty-bot.admin.bootstrap-email` | Vacío | Sólo para crear el primer usuario | Sí, dato sensible |
| `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD` | `beauty-bot.admin.bootstrap-password` | Vacío | Sólo para crear el primer usuario | Sí |
| `BEAUTY_BOT_ADMIN_SECURE_COOKIE` | `server.servlet.session.cookie.secure` | `false` | Sí en HTTPS productivo | No |

## 2. Detalle de cada property

### `BEAUTY_BOT_CLINIC_ID`

Identificador estable de la clínica. Se usa para:

1. Separar promociones.
2. Crear el primer administrador.
3. Asociar leads.
4. Construir el contexto conversacional.

Debe ser un entero positivo. No cambiarlo para renombrar una clínica: los administradores y promociones existentes conservarán el ID anterior y parecerán desaparecer del panel.

Ejemplo:

```dotenv
BEAUTY_BOT_CLINIC_ID=1
```

### `BEAUTY_BOT_ADMIN_ENABLED`

Controla la ejecución del bootstrap de administradores. Con `false` no se crea el usuario inicial, pero los usuarios ya persistidos no se borran y pueden seguir autenticándose.

```dotenv
BEAUTY_BOT_ADMIN_ENABLED=true
```

No utilizar esta variable como único mecanismo para ocultar el panel. La exposición externa también debe controlarse con el reverse proxy, firewall y autenticación.

### `BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL`

Email del primer administrador. Se normaliza a minúsculas. Sólo se crea si no existe otro usuario con ese email.

```dotenv
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=owner@clinica.com
```

El valor no debe quedar permanentemente en el manifiesto de despliegue.

### `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD`

Contraseña del primer administrador. Debe contener al menos 12 caracteres. Spring la persiste hasheada mediante `PasswordEncoder`; nunca se guarda el valor plano.

```dotenv
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=usar-un-secreto-largo-y-unico
```

Volver a iniciar con otra contraseña no modifica un usuario existente. La variable sólo aprovisiona, no funciona como reset de contraseña.

### `BEAUTY_BOT_ADMIN_SECURE_COOKIE`

Define el atributo `Secure` de la cookie de sesión.

| Ambiente | Valor |
|---|---|
| Local con HTTP | `false` |
| Test detrás de HTTPS | `true` |
| Producción HTTPS | `true` |

Con `true`, el navegador no envía la cookie por HTTP. Configurarlo en `true` localmente provoca que el login parezca perder la sesión.

La cookie también queda configurada como `HttpOnly` y `SameSite=Lax`; esos dos valores son fijos en `application.yml`.

## 3. Property nueva del frontend

| Variable | Default | Momento de lectura | Secreto |
|---|---|---|---|
| `VITE_API_BASE_URL` | `/whatsapp-ai-response-service/v1` | Build o arranque de Vite | No |

Ejemplo local:

```dotenv
VITE_API_BASE_URL=/whatsapp-ai-response-service/v1
```

Se recomienda usar una ruta relativa y servir frontend y backend bajo el mismo dominio. Esto evita configuración CORS adicional y permite usar la cookie `SameSite=Lax`.

Si se cambia después de ejecutar `npm run build`, hay que reconstruir la imagen del frontend porque Vite la inserta en los archivos estáticos.

## 4. Variables existentes relacionadas

Estas variables no fueron creadas por el módulo de promociones, pero afectan su prueba o despliegue:

| Variable | Uso |
|---|---|
| `PORT` | Debe ser `8081` en local para coincidir con el proxy de Vite |
| `SPRING_DATASOURCE_URL` | Base donde viven promociones y usuarios |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL |
| `SPRING_FLYWAY_ENABLED` | Debe permanecer `true` para aplicar V8 y V9 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Recomendado `validate` |
| `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED` | Habilita `/chat/test`; sólo local o QA |
| `BEAUTY_BOT_INTERNAL_API_KEY_ENABLED` | Protege endpoints internos, no el panel de sesión |
| `BEAUTY_BOT_INTERNAL_API_KEY_HEADER` | Nombre del header interno |
| `BEAUTY_BOT_INTERNAL_API_KEY` | Valor del API key interno |
| `BEAUTY_BOT_AI_ENABLED` | Habilita integración con OpenAI |
| `BEAUTY_BOT_AI_DECISION_ENABLED` | Permite que IA seleccione códigos activos |
| `BEAUTY_BOT_AI_DECISION_PROMPT_ID` | Prompt remoto de decisión |
| `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION` | Versión publicada del prompt |
| `WHATSAPP_ENABLED` | Habilita webhook y envío real por Meta |

## 5. Configuración local mínima

Backend con H2, sin IA, Calendar ni WhatsApp:

```powershell
$env:PORT = "8081"
$env:BEAUTY_BOT_CLINIC_ID = "1"
$env:BEAUTY_BOT_ADMIN_ENABLED = "true"
$env:BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL = "admin.local@clinica.com"
$env:BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD = "clave-local-segura-123"
$env:BEAUTY_BOT_ADMIN_SECURE_COOKIE = "false"
$env:BEAUTY_BOT_TEST_ENDPOINTS_ENABLED = "true"
$env:BEAUTY_BOT_INTERNAL_API_KEY_ENABLED = "false"
$env:BEAUTY_BOT_AI_ENABLED = "false"
$env:BEAUTY_BOT_AI_DECISION_ENABLED = "false"
$env:BEAUTY_BOT_CALENDAR_ENABLED = "false"
$env:WHATSAPP_ENABLED = "false"
```

Frontend, en `admin-web/.env.local`:

```dotenv
VITE_API_BASE_URL=/whatsapp-ai-response-service/v1
```

El archivo `.env.local` está ignorado por Git.

## 6. Configuración de QA con PostgreSQL

```dotenv
PORT=8081
BEAUTY_BOT_CLINIC_ID=1
BEAUTY_BOT_ADMIN_ENABLED=true
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=admin.qa@clinica.com
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=${SECRET_ADMIN_BOOTSTRAP}
BEAUTY_BOT_ADMIN_SECURE_COOKIE=true

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-qa:5432/beautybot
SPRING_DATASOURCE_USERNAME=beautybot
SPRING_DATASOURCE_PASSWORD=${SECRET_DATABASE_PASSWORD}
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_HEADER=Authorization
BEAUTY_BOT_INTERNAL_API_KEY=${SECRET_INTERNAL_API_KEY}
BEAUTY_BOT_AI_ENABLED=false
BEAUTY_BOT_AI_DECISION_ENABLED=false
WHATSAPP_ENABLED=false
```

La sintaxis `${SECRET_...}` representa una referencia conceptual al secret manager. La forma exacta depende de Docker, Kubernetes o la plataforma utilizada.

## 7. Configuración productiva recomendada

```dotenv
PORT=8081
BEAUTY_BOT_CLINIC_ID=1
BEAUTY_BOT_ADMIN_ENABLED=true
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=
BEAUTY_BOT_ADMIN_SECURE_COOKIE=true

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-prod:5432/beautybot
SPRING_DATASOURCE_USERNAME=beautybot
SPRING_DATASOURCE_PASSWORD=${SECRET_DATABASE_PASSWORD}
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=false
BEAUTY_BOT_INTERNAL_API_KEY_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_HEADER=Authorization
BEAUTY_BOT_INTERNAL_API_KEY=${SECRET_INTERNAL_API_KEY}
BEAUTY_BOT_AI_ENABLED=true
BEAUTY_BOT_AI_DECISION_ENABLED=true
BEAUTY_BOT_AI_DECISION_PROMPT_ID=pmpt_xxx
BEAUTY_BOT_AI_DECISION_PROMPT_VERSION=version_publicada
WHATSAPP_ENABLED=true
```

Las credenciales bootstrap deben estar vacías después de comprobar que el primer usuario quedó persistido.

## 8. Secuencia segura de bootstrap

1. Crear los secretos de email y contraseña en el gestor del ambiente.
2. Desplegar una instancia con ambos valores configurados.
3. Confirmar el log de creación del administrador.
4. Iniciar sesión y verificar el `clinicId` correcto.
5. Retirar ambos secretos del manifiesto.
6. Redesplegar.
7. Confirmar que el usuario existente sigue funcionando.
8. Confirmar que el log indica que no hay credenciales bootstrap configuradas.

Si se utiliza H2 en memoria, el usuario se pierde al reiniciar. Esta secuencia sólo es persistente con PostgreSQL u otra base persistente.

## 9. Base y migraciones

No existe una property específica para crear las tablas. Flyway aplica automáticamente:

| Migración | Objetos |
|---|---|
| V8 | `PROMOTIONS`, `PROMOTION_ALIASES`, `PROMOTION_EVENTS` |
| V9 | `ADMIN_USERS` |

Configuración obligatoria:

```dotenv
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

No cambiar `ddl-auto` a `create` o `update` en producción. El historial debe permanecer bajo control de Flyway.

## 10. Configuración del prompt

El backend siempre toma el cuerpo final desde la base. La IA sólo puede devolver códigos en `matchedPromotionCodes`.

Para habilitar selección semántica:

1. Publicar el contenido actualizado de `docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md`.
2. Configurar el ID del prompt.
3. Configurar la versión publicada.
4. Reiniciar el backend para cargar las properties.
5. Ejecutar casos con código, alias, múltiples promociones y mensajes ambiguos.

Sin prompt actualizado sigue funcionando el matching determinista por código y alias.

## 11. Verificación de configuración

Comprobar la versión del artefacto:

```powershell
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

Resultado esperado:

```text
2.0.0
```

Comprobar la versión de una instancia en ejecución:

```powershell
$base = "https://host/whatsapp-ai-response-service/v1"
$info = Invoke-RestMethod "$base/actuator/info" -Headers @{ Authorization = "Bearer api-key-interna" }
$info.build.version
```

El `spring-boot-maven-plugin` genera `META-INF/build-info.properties`; por eso `/actuator/info` informa la versión del binario desplegado y no una variable configurada manualmente.

Listar variables no secretas de la terminal:

```powershell
Get-ChildItem Env: | Where-Object {
    $_.Name -in @(
        "PORT",
        "BEAUTY_BOT_CLINIC_ID",
        "BEAUTY_BOT_ADMIN_ENABLED",
        "BEAUTY_BOT_ADMIN_SECURE_COOKIE"
    )
}
```

No imprimir `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD` ni passwords de base en logs o evidencia de QA.

## 12. Reglas de versionado

Desde esta entrega, todo cambio funcional, de configuración, contrato, esquema o despliegue debe actualizar la versión del proyecto en `pom.xml`.

Se utiliza SemVer:

| Tipo | Cuándo usarlo | Ejemplo desde `2.0.0` |
|---|---|---|
| Major | Cambio incompatible o arquitectura nueva | `3.0.0` |
| Minor | Funcionalidad compatible | `2.1.0` |
| Patch | Corrección compatible | `2.0.1` |

Checklist obligatorio para cada entrega:

1. Actualizar `<version>` en `pom.xml`.
2. Crear o actualizar documentación bajo `docs/vX.Y.Z/`.
3. Registrar properties agregadas, modificadas o eliminadas.
4. Registrar nuevas migraciones Flyway.
5. Ejecutar backend y frontend.
6. Guardar evidencia de pruebas asociada a esa versión.
7. No reutilizar una versión ya desplegada para código diferente.

La versión `2.0.0` corresponde a la incorporación del catálogo persistente, API administrativa, autenticación por sesión, CSRF, panel web e integración conversacional sin HTTP interno.

## 13. Matriz de valores

| Variable | Local | QA HTTPS | Producción HTTPS |
|---|---|---|---|
| `PORT` | `8081` | `8081` o plataforma | `8081` o plataforma |
| `BEAUTY_BOT_CLINIC_ID` | ID QA | ID QA | ID real estable |
| `BEAUTY_BOT_ADMIN_ENABLED` | `true` | `true` | `true` |
| Bootstrap email | Configurado | Sólo primer deploy | Sólo primer deploy |
| Bootstrap password | Configurado | Secret primer deploy | Secret primer deploy |
| `BEAUTY_BOT_ADMIN_SECURE_COOKIE` | `false` | `true` | `true` |
| `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED` | `true` | Según política | `false` |
| `SPRING_FLYWAY_ENABLED` | `true` | `true` | `true` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` | `validate` | `validate` |
| `VITE_API_BASE_URL` | Ruta relativa | Ruta relativa | Ruta relativa |
