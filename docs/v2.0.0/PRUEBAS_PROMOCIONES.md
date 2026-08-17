# Guía de pruebas de promociones

## Control de cambios y versiones

Este es el archivo central de control de versiones del proyecto. Cada entrega debe agregar una fila y enlazar su documento operativo sin borrar el historial anterior. La version actual es `2.3.2`.

| Version | Cambio principal | Documento |
|---|---|---|
| `2.0.0` | Panel y catalogo dinamico de promociones | Esta guia y [properties](./PROPERTIES_PROMOCIONES.md) |
| `2.1.0` | Deploy unificado de frontend y backend en Railway | [Deploy Railway](../v2.1.0/RAILWAY_DEPLOY.md) |
| `2.1.1` | Nueva paleta visual del panel | [Cambio de paleta](../v2.1.1/CAMBIO_PALETA_ADMIN.md) |
| `2.2.0` | Una entrega de cada promocion por conversacion | [Regla de entrega](../v2.2.0/PROMOCIONES_UNA_VEZ_POR_CONVERSACION.md) |
| `2.2.1` | SQL y operacion de las tablas de promociones | [SQL y verificacion](../v2.2.1/PROMOCIONES_UNA_VEZ_POR_CONVERSACION.md) |
| `2.3.0` | Informacion general de tratamientos y agenda manual | [IA medica y agenda manual](../v2.3.0/IA_MEDICA_Y_AGENDA_MANUAL.md) |
| `2.3.1` | Properties completas para Railway | [Railway properties](../v2.3.1/RAILWAY_PROPERTIES.md) |
| `2.3.2` | Correccion de listado de promociones en PostgreSQL | [Fix listado PostgreSQL](../v2.3.2/PROMOTION_LIST_POSTGRES_FIX.md) |

---

| Dato | Valor |
|---|---|
| Versión | `2.0.0` |
| Backend | Spring Boot, Java 21 |
| Frontend | `admin-web`, React y Vite |
| Alcance | Catálogo, seguridad, matching y respuesta conversacional |

Esta guía valida el circuito completo: creación desde el panel, persistencia, publicación, detección de una o varias promociones y uso del texto canónico por el bot sin reiniciar el backend.

## 1. Resultado esperado

Al finalizar deben quedar comprobados estos puntos:

1. Un usuario anónimo no puede acceder al API administrativo.
2. El administrador puede iniciar y cerrar sesión.
3. Una promoción nueva se guarda como `DRAFT` y todavía no responde mensajes.
4. Una promoción `ACTIVE` responde por código o alias.
5. Dos promociones mencionadas en el mismo mensaje producen dos bloques de respuesta.
6. Editar o archivar impacta en el siguiente mensaje, sin reiniciar Spring Boot.
7. Una promoción futura o vencida no se utiliza.
8. La respuesta enviada contiene el texto almacenado, no el comando `/botox`.
9. Los cambios quedan auditados en `PROMOTION_EVENTS`.
10. El lead registra los códigos detectados en `METADATA`.

## 2. Prerrequisitos

| Requisito | Cómo verificarlo |
|---|---|
| Java 21 o superior | `java -version` |
| Maven disponible | `mvn -version` |
| Node compatible con Vite 8 | `node --version` |
| npm disponible | `npm --version` |
| Dependencia privada accesible | Maven debe poder descargar `beauty-bot-common` |
| Puerto backend libre | `8081` |
| Puerto frontend libre | `5173` |

Para una prueba rápida se puede usar H2, que es el datasource predeterminado. Para validar persistencia real y despliegue se debe repetir el recorrido con PostgreSQL.

## 3. Preparar el backend local

Abrir PowerShell en la raíz del repositorio y definir un ambiente aislado:

```powershell
$env:PORT = "8081"
$env:BEAUTY_BOT_CLINIC_ID = "1"
$env:BEAUTY_BOT_ADMIN_ENABLED = "true"
$env:BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL = "admin.qa@clinica.com"
$env:BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD = "clave-local-segura-123"
$env:BEAUTY_BOT_ADMIN_SECURE_COOKIE = "false"
$env:BEAUTY_BOT_TEST_ENDPOINTS_ENABLED = "true"
$env:BEAUTY_BOT_INTERNAL_API_KEY_ENABLED = "false"
$env:BEAUTY_BOT_AI_ENABLED = "false"
$env:BEAUTY_BOT_AI_DECISION_ENABLED = "false"
$env:BEAUTY_BOT_CALENDAR_ENABLED = "false"
$env:WHATSAPP_ENABLED = "false"
```

Estas variables sólo afectan la terminal actual. Copiar `.env.example` a `.env` no hace que Maven lo lea automáticamente; IntelliJ, Docker o el sistema de despliegue deben inyectarlo explícitamente.

Iniciar el backend:

```powershell
mvn spring-boot:run
```

Confirmar en los logs:

1. Flyway llega a la versión `9`.
2. Hibernate valida el esquema sin errores.
3. Aparece `Initial admin user created for clinicId=1` la primera vez.
4. La aplicación escucha en el puerto `8081`.

Comprobar salud:

```powershell
$base = "http://localhost:8081/whatsapp-ai-response-service/v1"
Invoke-RestMethod "$base/actuator/health"
```

El resultado esperado es `status = UP`.

Comprobar la versión realmente ejecutada:

```powershell
$info = Invoke-RestMethod "$base/actuator/info"
$info.build.version
```

El resultado esperado es `2.0.0`. Si no coincide, se está probando otro artefacto aunque el código local tenga la versión correcta.

## 4. Preparar el panel

Abrir otra terminal:

```powershell
cd admin-web
npm ci
npm run dev
```

Abrir `http://localhost:5173`.

Vite redirige `/whatsapp-ai-response-service/v1` a `http://localhost:8081`. Si el backend usa otro puerto, hay que modificar el proxy o ejecutar ambos con los valores documentados.

## 5. Probar autenticación y sesión

1. Intentar ingresar con una contraseña incorrecta.
2. Verificar que el panel muestre un error y no abra el catálogo.
3. Ingresar con `admin.qa@clinica.com` y `clave-local-segura-123`.
4. Verificar que aparezca el catálogo de promociones.
5. Refrescar el navegador.
6. Confirmar que la sesión continúa activa.
7. Cerrar sesión desde el panel.
8. Confirmar que vuelve a la pantalla de login.
9. Usar el botón Atrás del navegador.
10. Confirmar que el API no vuelve a mostrar datos sin iniciar sesión.

Resultado esperado: credenciales inválidas devuelven `401`, una sesión válida sobrevive al refresh y el logout invalida `JSESSIONID`.

## 6. Crear la primera promoción

Ingresar nuevamente y seleccionar `Nueva promoción`. Completar:

| Campo | Valor de prueba |
|---|---|
| Código | `botox-qa-v2` |
| Título | `Botox QA versión 2` |
| Alias | `botox qa, toxina qa, promo botox qa` |
| Respuesta | `QA_BOTOX_V2: promoción canónica de Botox.` |
| Disponible desde | Vacío |
| Disponible hasta | Vacío |

Presionar `Crear borrador`.

Verificar:

1. El estado visible es `Borrador`.
2. El código queda normalizado sin `/` y sin espacios.
3. La vista previa conserva exactamente el cuerpo configurado.
4. El probador de mensajes no devuelve la promoción mientras siga en `DRAFT`.

## 7. Publicar y probar matching

Presionar `Publicar` sobre la promoción anterior.

Probar estos mensajes en `Probador de detección`:

| Mensaje | Resultado esperado |
|---|---|
| `/botox-qa-v2` | Una coincidencia: `botox-qa-v2` |
| `Quiero la promo botox qa` | Una coincidencia: `botox-qa-v2` |
| `Me interesa TOXÍNA QA` | Una coincidencia, ignorando mayúsculas y acentos |
| `Consulta que no menciona tratamientos` | Cero coincidencias |

El matcher compara frases completas. Un alias demasiado genérico, por ejemplo `promo`, generará falsos positivos y no debe utilizarse.

## 8. Probar dos promociones en un mensaje

Crear y publicar una segunda promoción:

| Campo | Valor de prueba |
|---|---|
| Código | `rino-qa-v2` |
| Título | `Rinomodelado QA versión 2` |
| Alias | `rino qa, nariz qa, rinomodelado qa` |
| Respuesta | `QA_RINO_V2: promoción canónica de Rinomodelado.` |

Probar:

```text
Vi la promo botox qa y también la de nariz qa
```

Resultado esperado:

1. El probador devuelve `botox-qa-v2` y `rino-qa-v2`.
2. El orden coincide con el orden de aparición en el mensaje.
3. En el flujo conversacional se unen ambos cuerpos con una línea en blanco.
4. Nunca se envían literalmente `/botox-qa-v2` o `/rino-qa-v2` como respuesta.

## 9. Probar el flujo conversacional completo

Este endpoint existe sólo porque se inició el backend con `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED=true`.

```powershell
$base = "http://localhost:8081/whatsapp-ai-response-service/v1"
$body = @{
    phoneNumber = "5491100002001"
    message = "Quiero la promo botox qa y también rinomodelado qa"
} | ConvertTo-Json

$result = Invoke-RestMethod `
    -Method Post `
    -Uri "$base/chat/test" `
    -ContentType "application/json" `
    -Body $body

$result | ConvertTo-Json -Depth 5
```

Verificar que `reply` contiene los dos marcadores:

```text
QA_BOTOX_V2
QA_RINO_V2
```

La respuesta también puede incluir la siguiente pregunta del proceso de captura del lead. Eso es correcto.

Para repetir una prueba desde una conversación limpia, cambiar `phoneNumber`. Una conversación que ya llegó a `HUMAN_HANDOFF` no vuelve a ser respondida automáticamente.

Si `BEAUTY_BOT_INTERNAL_API_KEY_ENABLED=true`, agregar el header configurado:

```powershell
-Headers @{ Authorization = "Bearer valor-configurado" }
```

## 10. Probar cambios sin reinicio

1. Mantener el backend ejecutándose.
2. Editar `botox-qa-v2` desde el panel.
3. Cambiar el cuerpo a `QA_BOTOX_EDITADA: texto actualizado sin reinicio.`.
4. Guardar.
5. Enviar un mensaje con un teléfono nuevo que contenga `botox qa`.
6. Confirmar que la respuesta contiene `QA_BOTOX_EDITADA`.
7. Confirmar que ya no contiene `QA_BOTOX_V2`.
8. Archivar la promoción.
9. Enviar otro mensaje con otro teléfono nuevo.
10. Confirmar que la promoción archivada ya no aparece.

Este escenario demuestra que el bot consulta la base en cada mensaje y no requiere recargar properties ni reiniciar módulos.

## 11. Probar vigencia

Ejecutar los siguientes casos por separado:

| Caso | Configuración | Resultado esperado |
|---|---|---|
| Sin fechas | Ambas fechas vacías | Disponible hasta archivar |
| Programada | `validFrom` futuro y estado `ACTIVE` | Visible como programada, no matchea todavía |
| Vigente | Hora actual entre ambas fechas | Matchea |
| Vencida | `validUntil` pasado | No matchea |
| Rango inválido | Fin anterior o igual al inicio | Rechazo de validación |
| Activar vencida | Borrador con fin pasado | Rechazo al publicar |

El navegador convierte fechas locales a ISO-8601 y el backend persiste instantes UTC.

## 12. Probar validaciones y concurrencia

| Prueba | Resultado esperado |
|---|---|
| Código duplicado en la misma clínica | `409 Conflict` |
| Código de un carácter | `400 Bad Request` |
| Título vacío | `400 Bad Request` |
| Texto mayor a 1800 caracteres | `400 Bad Request` |
| Más de 30 alias | `400 Bad Request` |
| Alias menor a 2 caracteres | `400 Bad Request` |
| Actualización con `version` vieja | `409 Conflict` |
| Endpoint admin sin sesión | `401 Unauthorized` |
| `POST` admin sin CSRF | `403 Forbidden` |

Cuando dos usuarios editan simultáneamente, el segundo debe recargar la promoción antes de volver a guardar.

## 13. Prueba directa del API con PowerShell

Este recorrido evita depender de la interfaz y valida cookie, CSRF y contrato REST:

Antes de repetirlo, cambiar el sufijo del código `api-botox-v2`. Los códigos permanecen reservados aunque una promoción haya sido archivada, porque no existe borrado físico.

```powershell
$base = "http://localhost:8081/whatsapp-ai-response-service/v1"
$email = "admin.qa@clinica.com"
$password = "clave-local-segura-123"

# 1. Crear cookie anónima y obtener CSRF.
$csrf = Invoke-RestMethod `
    -Uri "$base/api/admin/csrf" `
    -SessionVariable adminSession
$headers = @{}
$headers[$csrf.headerName] = $csrf.token

# 2. Iniciar sesión.
$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$base/api/admin/login" `
    -WebSession $adminSession `
    -Headers $headers `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{ username = $email; password = $password }

# 3. El login rota CSRF; pedir uno nuevo.
$csrf = Invoke-RestMethod `
    -Uri "$base/api/admin/csrf" `
    -WebSession $adminSession
$headers = @{}
$headers[$csrf.headerName] = $csrf.token

# 4. Crear un borrador.
$payload = @{
    code = "api-botox-v2"
    title = "API Botox V2"
    messageBody = "QA_API_BOTOX: cuerpo leído desde base de datos."
    aliases = @("api botox", "api toxina")
    validFrom = $null
    validUntil = $null
} | ConvertTo-Json

$promotion = Invoke-RestMethod `
    -Method Post `
    -Uri "$base/api/admin/promotions" `
    -WebSession $adminSession `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $payload

# 5. Publicar usando la versión devuelta.
$promotion = Invoke-RestMethod `
    -Method Post `
    -Uri "$base/api/admin/promotions/$($promotion.id)/activate" `
    -WebSession $adminSession `
    -Headers $headers `
    -ContentType "application/json" `
    -Body (@{ version = $promotion.version } | ConvertTo-Json)

# 6. Probar detección.
$match = Invoke-RestMethod `
    -Method Post `
    -Uri "$base/api/admin/promotions/match-preview" `
    -WebSession $adminSession `
    -Headers $headers `
    -ContentType "application/json" `
    -Body (@{ message = "Quiero api toxina" } | ConvertTo-Json)

$match.matches | Format-Table code, title, messageBody
```

El resultado final debe incluir `api-botox-v2` y el cuerpo `QA_API_BOTOX`.

## 14. Verificar la base PostgreSQL

Confirmar migraciones:

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('8', '9')
ORDER BY installed_rank;
```

Resultado esperado: V8 y V9 con `success = true`.

Revisar promociones:

```sql
SELECT id, clinic_id, code, status, valid_from, valid_until, version, updated_by
FROM promotions
ORDER BY updated_at DESC;
```

Revisar auditoría:

```sql
SELECT promotion_id, event_type, description, created_by, created_at
FROM promotion_events
ORDER BY created_at DESC;
```

Revisar los códigos asociados al lead:

```sql
SELECT phone_number, metadata
FROM leads
WHERE metadata LIKE '%matchedPromotionCodes%'
ORDER BY updated_at DESC;
```

No editar manualmente `version`, estados o eventos durante una prueba funcional.

## 15. Prueba opcional con WhatsApp real

1. Repetir el arranque con `WHATSAPP_ENABLED=true` y las credenciales de Meta.
2. Mantener activas las dos promociones QA.
3. Enviar desde un número de prueba: `Quiero botox qa y rino qa`.
4. Confirmar en WhatsApp que llegan ambos textos canónicos.
5. Confirmar que no llega `/botox-qa-v2` ni `/rino-qa-v2`.
6. Editar uno de los cuerpos desde el panel.
7. Enviar desde una conversación nueva y verificar el cambio inmediato.
8. Revisar `LEADS.METADATA` y `PROMOTION_EVENTS`.

## 16. Regresión mínima

Después de las pruebas manuales ejecutar:

```powershell
mvn clean test
cd admin-web
npm ci
npm run build
```

Para la versión `2.0.0`, la suite de backend debe ejecutar 89 pruebas sin fallos. El número puede aumentar en versiones posteriores, pero nunca debe disminuir sin una justificación registrada.

También verificar:

1. Un saludo sin promoción mantiene el flujo normal.
2. Una solicitud de humano no es reemplazada por una promoción.
3. Una consulta médica no es reemplazada por una promoción.
4. Una queja no es reemplazada por una promoción.
5. Los webhooks siguen aceptando y procesando mensajes.
6. El panel funciona en desktop y mobile sin scroll horizontal.

## 17. Problemas frecuentes

| Síntoma | Causa probable | Acción |
|---|---|---|
| El panel no conecta | Backend en otro puerto | Usar `8081` o ajustar el proxy |
| Login devuelve `401` | Credenciales incorrectas o usuario no creado | Revisar bootstrap y base utilizada |
| Mutación devuelve `403` | CSRF ausente o vencido | Solicitar `/api/admin/csrf` nuevamente |
| El login funciona pero se pierde | Cookie `Secure` sobre HTTP local | Usar `BEAUTY_BOT_ADMIN_SECURE_COOKIE=false` localmente |
| No aparece la promoción | Está en borrador, archivada, futura o vencida | Revisar estado y vigencia |
| No detecta el mensaje | El texto no contiene código ni alias completo | Agregar un alias específico |
| Detecta mensajes incorrectos | Alias demasiado genérico | Reemplazarlo por una frase más precisa |
| Cambió `CLINIC_ID` y desapareció el catálogo | Los datos están aislados por clínica | Restaurar el ID correcto; no cambiarlo como migración |
| Reiniciar H2 borra usuarios y promociones | H2 está en memoria | Usar PostgreSQL para persistencia |
| Guardar devuelve `409` | Otra edición incrementó `version` | Recargar y volver a editar |

La detección determinista es léxica. Frases como `no quiero botox` pueden contener igualmente un alias; los casos de negación o intención ambigua deben probarse con el prompt de IA real antes de producción.

## 18. Evidencia a guardar

Por cada ambiente conservar:

1. Versión obtenida desde el POM y desde `/actuator/info`.
2. Resultado de `mvn test`.
3. Resultado de `npm run build`.
4. Captura del login y del catálogo.
5. Captura de la prueba con dos promociones.
6. Filas de V8/V9 en `flyway_schema_history`.
7. Eventos de creación, actualización, publicación y archivo.
8. Ejemplo de `LEADS.METADATA` con `matchedPromotionCodes`.
9. Fecha, ambiente y responsable de la validación.
