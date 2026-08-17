# Administración de promociones

Documentación operativa de la versión `2.0.0`:

- [Guía detallada de pruebas](v2.0.0/PRUEBAS_PROMOCIONES.md)
- [Properties y configuración](v2.0.0/PROPERTIES_PROMOCIONES.md)

## Arquitectura

El repositorio contiene dos aplicaciones desplegables:

- `src/main/java/.../promotion`: módulo de promociones dentro del monolito Spring Boot.
- `admin-web`: panel React/Vite para administrar el catálogo.

El bot no hace una llamada HTTP para consultar promociones. `HandleIncomingMessageUseCase` consume la interfaz pública `PromotionCatalog` en el mismo proceso y el módulo resuelve los datos desde la base compartida.

```text
WhatsApp -> Spring Boot -> PromotionCatalog -> PostgreSQL
                    |             |
                    |             +-> textos canónicos activos
                    +-> respuesta final -> WhatsApp

Navegador -> /api/admin/promotions -> PromotionManagement -> PostgreSQL
```

Las clases de otros dominios sólo deben importar tipos del paquete público `promotion`. Entidades, repositorios y servicios de persistencia permanecen bajo `promotion.internal`.

## Comportamiento

Cada promoción tiene un código, título, texto de respuesta, alias, estado y una vigencia opcional. Su ciclo de vida es:

```text
DRAFT -> ACTIVE -> ARCHIVED
```

Sólo una promoción `ACTIVE`, cuya fecha actual esté dentro de su vigencia, puede ser usada por el bot. Crear, editar, publicar o archivar tiene efecto en el siguiente mensaje; no requiere reiniciar el backend.

El código y los alias se comparan como frases completas, sin distinguir mayúsculas ni acentos. Un mensaje puede coincidir con varias promociones y el backend une los textos en el orden en que aparecen. El modelo de IA sólo puede seleccionar códigos activos; el texto enviado siempre sale de la base de datos.

## Seguridad

El panel usa login de Spring Security, cookie de sesión `HttpOnly` y protección CSRF. Los endpoints bajo `/api/admin/**` requieren un usuario `ADMIN` o `EDITOR`. El `clinicId` se obtiene de la sesión autenticada, nunca de un header enviado por el navegador.

Para crear el primer usuario, configurar temporalmente:

```dotenv
BEAUTY_BOT_CLINIC_ID=1
BEAUTY_BOT_ADMIN_BOOTSTRAP_EMAIL=owner@example.com
BEAUTY_BOT_ADMIN_BOOTSTRAP_PASSWORD=una-clave-de-al-menos-12-caracteres
```

Después del primer arranque, retirar las dos credenciales bootstrap del entorno. El usuario persistido continúa funcionando. En producción con HTTPS también debe configurarse:

```dotenv
BEAUTY_BOT_ADMIN_SECURE_COOKIE=true
```

## Desarrollo local

Iniciar el backend:

```powershell
mvn spring-boot:run
```

Iniciar el panel en otra terminal:

```powershell
cd admin-web
npm install
npm run dev
```

Vite redirige `/whatsapp-ai-response-service/v1` a `http://localhost:8081`. Para producción, servir ambos detrás del mismo dominio y enrutar esa ruta al backend. Si se utiliza otra URL, definir `VITE_API_BASE_URL` al construir el panel.

## API administrativa

- `GET /api/admin/csrf`: obtiene el token CSRF.
- `POST /api/admin/login`: inicia sesión con `username` y `password`.
- `GET /api/admin/session`: devuelve el usuario autenticado.
- `GET /api/admin/promotions`: busca y pagina promociones.
- `POST /api/admin/promotions`: crea un borrador.
- `PUT /api/admin/promotions/{id}`: actualiza usando control optimista por `version`.
- `POST /api/admin/promotions/{id}/activate`: publica.
- `POST /api/admin/promotions/{id}/archive`: archiva.
- `POST /api/admin/promotions/match-preview`: prueba qué promociones detectaría un mensaje.

Flyway crea `PROMOTIONS`, `PROMOTION_ALIASES`, `PROMOTION_EVENTS` y `ADMIN_USERS` mediante las migraciones V8 y V9.
