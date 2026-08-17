# Deploy unificado en Railway

| Dato | Valor |
|---|---|
| Versión | `2.1.0` |
| Servicios | Una aplicación web y un PostgreSQL administrado |
| Panel | `/whatsapp-ai-response-service/v1/` |
| API | `/whatsapp-ai-response-service/v1/api/**` |

El `Dockerfile` compila `admin-web`, incorpora los archivos estáticos al JAR de Spring Boot y genera una sola imagen. De esta forma el panel y el backend comparten dominio, cookie de sesión y protección CSRF.

## 1. Crear el proyecto

1. Subir la rama a GitHub.
2. En Railway, crear un proyecto vacío.
3. Agregar `Database -> PostgreSQL`. El nombre usado en los ejemplos es `Postgres`.
4. Agregar un servicio desde el repositorio GitHub y seleccionar la rama a desplegar.
5. Mantener el directorio raíz del servicio en `/`. Railway detectará `Dockerfile` y `railway.json`.

El build descarga `beauty-bot-common` desde GitHub Packages. Configurar `GITHUB_USERNAME` y un `GITHUB_TOKEN` con permiso de lectura de paquetes como variables selladas del servicio.

## 2. Variables del servicio

Usar el editor RAW de Railway. Las referencias `${{Postgres.*}}` toman los valores del servicio PostgreSQL sin exponerlos en el repositorio.

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

BEAUTY_BOT_CLINIC_ID=1
BEAUTY_BOT_ADMIN_ENABLED=true
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=admin.qa@clinica.com
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=<clave-de-al-menos-12-caracteres>
BEAUTY_BOT_ADMIN_SECURE_COOKIE=true

BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_ENABLED=true
BEAUTY_BOT_INTERNAL_API_KEY_HEADER=Authorization
BEAUTY_BOT_INTERNAL_API_KEY=<secreto-interno>

BEAUTY_BOT_AI_ENABLED=false
BEAUTY_BOT_AI_DECISION_ENABLED=false
BEAUTY_BOT_CALENDAR_ENABLED=false
WHATSAPP_ENABLED=false
```

No es necesario definir `PORT`: Railway lo inyecta y Spring Boot ya consume esa variable. Tampoco se debe definir `VITE_API_BASE_URL`, porque el panel usa la ruta relativa del backend en el mismo origen.

## 3. Publicar y verificar

En `Settings -> Networking`, generar un dominio público. Railway desplegará nuevamente con cada push a la rama conectada.

Abrir:

```text
https://<dominio-railway>/whatsapp-ai-response-service/v1/
```

El healthcheck definido en `railway.json` usa esa misma ruta. No usa `/actuator/health` porque los endpoints Actuator requieren la API key interna en este ambiente.

Para consultar salud y versión manualmente:

```powershell
$base = "https://<dominio-railway>/whatsapp-ai-response-service/v1"
$headers = @{ Authorization = "<secreto-interno>" }

Invoke-RestMethod "$base/actuator/health" -Headers $headers
(Invoke-RestMethod "$base/actuator/info" -Headers $headers).build.version
```

Los resultados esperados son `UP` y `2.1.0`. En los logs también deben aparecer las migraciones Flyway V8 y V9 y la creación inicial del administrador.

## 4. Probar promociones

Seguir el catálogo desde el panel:

1. Iniciar sesión con el usuario bootstrap.
2. Crear `botox-qa-v2` como borrador y comprobar que no matchee.
3. Publicarlo y comprobar que `Quiero botox qa` sí matchee.
4. Editar su texto sin reiniciar el servicio y comprobar el cambio inmediato.
5. Archivarlo y comprobar que deje de matchear.

Para probar el flujo conversacional:

```powershell
$body = @{
    phoneNumber = "5491100003001"
    message = "Quiero botox qa"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "$base/chat/test" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $body
```

Usar un teléfono nuevo para cada recorrido. La respuesta debe contener el cuerpo persistido de la promoción, no el código de comando.

## 5. Cierre del QA

Después de comprobar el primer login, quitar `BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL` y `BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD` de Railway; el usuario ya quedó persistido. Antes de promover el ambiente a producción, establecer `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=false` y configurar las integraciones reales de IA, calendario y WhatsApp.
