# Documentacion para cliente - Microservicio de respuestas por WhatsApp

## 1. Resumen ejecutivo

Este microservicio funciona como un asistente automatico de WhatsApp para una clinica estetica. Su objetivo es responder consultas iniciales, entender que necesita la persona, pedir los datos basicos para avanzar y dejar el caso listo para que una asesora humana continue la atencion.

El servicio no reemplaza a la asesora ni confirma turnos de forma automatica. Actua como primer filtro: atiende rapido, ordena la informacion, consulta disponibilidad si la agenda esta conectada y deriva a una persona cuando corresponde.

## 2. Que hace el servicio

### Atencion automatica por WhatsApp

El microservicio recibe mensajes entrantes desde WhatsApp Cloud API y responde desde el mismo numero configurado en Meta. Puede atender consultas como:

- "Hola, quiero un turno para botox".
- "Donde estan ubicados?".
- "Que horarios tienen?".
- "Tenes disponibilidad el viernes a la tarde?".
- "Cuanto sale?".
- "Quiero hablar con una asesora".

### Recoleccion de datos del potencial cliente

Durante la conversacion, el bot intenta reunir los datos minimos para que la asesora pueda continuar sin empezar de cero:

- Telefono de WhatsApp.
- Nombre de la persona.
- Tratamiento de interes.
- Si es primera vez en la clinica.
- Preferencia de dia u horario.

Si falta algun dato, el bot pregunta de a uno por vez para que la conversacion sea simple.

### Respuestas informativas basicas

El bot puede responder informacion configurada del negocio:

- Nombre de la clinica.
- Direccion o sede.
- Dias y horarios de atencion.
- Consultas generales sobre tratamientos.
- Consultas de precios con respuesta controlada.

Por seguridad comercial, el servicio esta preparado para no inventar precios exactos. Ante preguntas de valores, promos o formas de pago, puede indicar que una asesora confirma la informacion vigente.

### Horarios y preferencia de fecha

El bot informa los dias y horarios de trabajo y la doctora configurada. Luego pregunta que fecha prefiere el paciente y la guarda en el lead para que una asesora coordine manualmente. No consulta, ofrece ni confirma disponibilidad de calendario.

### Derivacion a asesora humana

El servicio deriva la conversacion a una persona cuando:

- Ya se reunieron los datos minimos.
- El cliente pide hablar con una asesora.
- Hay una queja o reclamo.
- El mensaje requiere criterio medico o profesional.
- El cliente quiere cancelar o reprogramar.
- Hay que confirmar turno, pago, sena o comprobante.
- La consulta de agenda falla y no conviene inventar disponibilidad.

Cuando se deriva, el sistema puede enviar una notificacion por WhatsApp a un numero interno de asesora con un resumen del caso.

### Pausa del bot durante atencion humana

Cuando una conversacion queda en manos de una asesora, el bot deja de responder automaticamente a ese cliente. Si el cliente vuelve a escribir mientras esta en atencion humana, el sistema registra el mensaje y puede avisar a la asesora.

Luego, mediante una accion interna, se puede liberar la conversacion para que el bot vuelva a atender.

### Gestion de leads

Cada conversacion genera o actualiza un lead. El lead guarda informacion util para seguimiento comercial:

- Telefono.
- Nombre.
- Tratamiento de interes.
- Si es primera vez.
- Preferencia horaria.
- Estado del lead.
- Temperatura comercial: frio, tibio o caliente.
- Puntaje de prioridad.
- Notas.
- Usuario asignado.
- Fechas relevantes, por ejemplo cuando pidio turno o quedo listo para asesora.

El proyecto expone esta informacion por API interna. No incluye por si solo una pantalla de CRM para operadores, salvo que se conecte o construya una interfaz aparte.

## 3. Como funciona la conversacion

1. El cliente escribe al numero de WhatsApp de la clinica.
2. Meta envia el mensaje al microservicio.
3. El servicio valida que el mensaje sea correcto y evita procesar duplicados.
4. Se busca o crea la conversacion de ese telefono.
5. El sistema analiza el mensaje usando OpenAI o, si corresponde, un flujo local por reglas.
6. Se decide que hacer: responder, pedir un dato, consultar agenda o derivar a una asesora.
7. Se guarda el mensaje recibido y la respuesta enviada.
8. Se crea o actualiza el lead comercial.
9. Si requiere asesora, se envia una notificacion interna con el resumen del caso.

## 4. Que no hace actualmente

Este alcance es importante para evitar malentendidos:

- No confirma turnos automaticamente.
- No consulta ni crea eventos en Google Calendar.
- No cancela ni reprograma turnos por si solo.
- No da diagnosticos ni recomendaciones medicas.
- No informa precios exactos si no se define expresamente una politica para eso.
- No procesa pagos.
- No valida comprobantes de pago.
- No envia imagenes, videos ni archivos desde el bot.
- No reemplaza un CRM completo con pantalla de gestion.
- No administra multiples clinicas de forma completa; internamente trabaja con una clinica principal.

## 5. Funcionalidades que se pueden agregar

Ademas del alcance actual, el servicio puede crecer por etapas. Estas mejoras no forman parte del funcionamiento basico actual, pero son extensiones naturales del sistema.

### Agenda avanzada con Google Calendar

Hoy el bot puede consultar disponibilidad si Google Calendar esta conectado. Como siguiente etapa, se puede agregar que el sistema tambien gestione turnos dentro de la agenda.

Con esta mejora, el bot o la asesora podrian:

- Crear un turno en Google Calendar cuando el cliente confirma un horario.
- Guardar nombre, telefono, tratamiento y observaciones dentro del evento.
- Cancelar un turno cuando el cliente lo solicita.
- Reprogramar un turno moviendolo a otro dia u horario.
- Evitar doble reserva de horarios.
- Enviar una respuesta automatica confirmando que el turno quedo registrado.
- Dejar trazabilidad de quien creo, cancelo o modifico el turno.

Para esta etapa habria que definir reglas claras:

- Quien puede confirmar un turno: solo asesora, bot automatico o ambos.
- Que datos son obligatorios antes de agendar.
- Duracion de cada tratamiento.
- Si cada tratamiento usa una agenda distinta.
- Si hay profesionales, boxes o sedes diferentes.
- Politica de cancelacion y reprogramacion.
- Si se envia recordatorio antes del turno.

### Precios configurables desde una web

Actualmente el bot esta preparado para no inventar precios exactos. Una mejora posible es crear un modulo de precios administrable.

La idea seria que el cliente tenga una pantalla web simple donde pueda cargar y modificar:

- Tratamientos.
- Precio de lista.
- Promociones vigentes.
- Formas de pago.
- Vigencia de cada precio o promocion.
- Comentarios internos para asesoras.
- Si el precio puede ser informado por el bot o si debe derivarse a una asesora.

Con esto, el bot podria responder precios usando informacion cargada por el negocio, sin depender de cambios tecnicos cada vez que cambie una promocion.

Ejemplo de funcionamiento:

1. La clinica modifica el precio de un tratamiento desde la web.
2. El sistema guarda el nuevo valor.
3. Cuando un cliente consulta por ese tratamiento, el bot responde con el precio autorizado.
4. Si el tratamiento requiere evaluacion previa, el bot no informa precio cerrado y deriva a asesora.

### Panel web de administracion

Tambien se puede agregar una web interna para que el equipo de la clinica gestione el servicio sin tocar configuraciones tecnicas.

Ese panel podria incluir:

- Ver leads recibidos por WhatsApp.
- Filtrar leads por estado, tratamiento, temperatura o fecha.
- Asignar un lead a una asesora.
- Agregar notas internas.
- Cambiar estados, por ejemplo "contactado", "turno pedido", "turno confirmado", "perdido" o "cerrado".
- Ver resumen de conversaciones.
- Administrar tratamientos y precios.
- Administrar horarios, reglas de agenda y mensajes frecuentes.
- Ver metricas basicas de atencion y conversion.

Esto convertiria al microservicio en una herramienta mas operativa para el equipo comercial.

### Batch de envio de campanas

Otro servicio que se puede sumar es un modulo batch para enviar campanas por WhatsApp.

Un "batch" es un proceso que envia mensajes en cantidad de forma controlada. Sirve para acciones como:

- Promociones de tratamientos.
- Recordatorios a clientes antiguos.
- Campanas por fechas especiales.
- Recontacto de leads que no respondieron.
- Avisos de nuevos servicios.
- Mensajes segmentados por interes, por ejemplo clientes interesados en botox, limpieza facial o depilacion.

El envio deberia hacerse de forma ordenada para no saturar el numero de WhatsApp y respetar las reglas de Meta.

El modulo podria contemplar:

- Lista de destinatarios.
- Mensaje aprobado.
- Fecha y hora de envio.
- Cantidad maxima de mensajes por tanda.
- Pausas entre envios.
- Registro de enviados, fallidos y pendientes.
- Evitar enviar dos veces la misma campana al mismo contacto.
- Detener una campana si hay demasiados errores.

### API y web para crear campanas

Junto con el batch, se puede crear una API y una pantalla web para que el cliente arme campanas sin pedir cambios tecnicos.

Desde esa web, el equipo podria:

- Crear una campana nueva.
- Elegir el publico destinatario.
- Escribir o seleccionar el mensaje.
- Guardar borradores.
- Pedir aprobacion interna antes de enviar.
- Programar fecha y hora de envio.
- Pausar o cancelar una campana.
- Ver resultados.

La API permitiria que otros sistemas tambien creen campanas o carguen contactos, por ejemplo un CRM externo, una planilla importada o una web de administracion.

### Segmentacion de contactos

Para que las campanas sean mas utiles, se puede sumar segmentacion. Esto significa elegir a quienes enviar segun informacion disponible.

Ejemplos:

- Clientes que consultaron por un tratamiento especifico.
- Leads calientes que todavia no confirmaron turno.
- Personas que pidieron precio y no respondieron.
- Clientes que ya se atendieron antes.
- Contactos cargados manualmente desde una planilla.
- Personas que aceptaron recibir comunicaciones.

Esta parte es importante para que las campanas sean mas relevantes y no se envien mensajes innecesarios.

### Plantillas y aprobaciones de WhatsApp

Para enviar campanas por WhatsApp normalmente se usan plantillas aprobadas por Meta. Se puede agregar una gestion de plantillas para ordenar ese proceso.

El sistema podria guardar:

- Nombre de la plantilla.
- Texto aprobado.
- Idioma.
- Variables editables, por ejemplo nombre, tratamiento o promocion.
- Estado de aprobacion.
- Tipo de uso: promocion, recordatorio, seguimiento o informacion.

Esto ayuda a que las campanas salgan con mensajes autorizados y consistentes.

### Reportes y metricas comerciales

Otra mejora posible es sumar reportes para medir resultados.

Algunos ejemplos:

- Cantidad de mensajes recibidos.
- Cantidad de leads generados.
- Tratamientos mas consultados.
- Cuantos leads fueron derivados a asesora.
- Cuantos pidieron turno.
- Cuantos turnos se confirmaron.
- Rendimiento de campanas.
- Mensajes enviados, entregados, fallidos y respondidos.
- Tiempo promedio hasta que una asesora toma el caso.

Estos reportes ayudan a evaluar si el bot esta generando oportunidades reales y donde conviene ajustar la comunicacion.

### Integracion con otros sistemas

Si el cliente ya usa otras herramientas, el servicio puede integrarse con ellas.

Ejemplos:

- CRM existente.
- Sistema de turnos.
- Planillas de Google Sheets.
- Plataforma de email marketing.
- Sistema de pagos.
- Dashboard de reportes.
- Web institucional o landing pages.

Estas integraciones permitirian que la informacion no quede aislada y que el equipo trabaje desde sus herramientas habituales.

### Recomendacion de orden para futuras etapas

Un orden razonable de evolucion seria:

1. Consolidar el bot actual con WhatsApp, OpenAI, leads y disponibilidad.
2. Agregar agenda avanzada para crear, cancelar y reprogramar turnos.
3. Crear panel web para leads, precios y configuraciones comerciales.
4. Sumar precios configurables y tratamientos administrables.
5. Agregar modulo de campanas con batch de envio.
6. Agregar web y API para crear, programar y revisar campanas.
7. Incorporar reportes, segmentacion avanzada e integraciones externas.

De esta forma, cada etapa agrega valor concreto sin mezclar demasiados cambios al mismo tiempo.

## 6. Informacion que debe entregar el cliente

### Datos comerciales de la clinica

- Nombre comercial de la clinica.
- Direccion o sede que debe informar el bot.
- Dias y horarios de atencion.
- Lista de tratamientos principales.
- Preguntas frecuentes y respuestas aprobadas.
- Politica para precios: si se informan, si se derivan o si se responden solo de forma general.
- Tono de comunicacion esperado: formal, cercano, neutro, etc.
- Casos en los que siempre debe intervenir una asesora.

### Datos para WhatsApp

El cliente debe contar con una configuracion de Meta/WhatsApp Cloud API o autorizarnos a configurarla:

- Cuenta de Meta Business.
- App de Meta con producto WhatsApp habilitado.
- Numero de WhatsApp que se usara para el bot.
- Phone Number ID del numero en Meta.
- Token de acceso de Meta para enviar mensajes.
- App Secret de Meta.
- Verify Token acordado para validar el webhook.
- Permiso para configurar el webhook del servicio.
- Numero de WhatsApp interno de la asesora o equipo que recibira notificaciones.

### Datos para OpenAI

Para usar respuestas con IA se necesita:

- Cuenta de OpenAI Platform con facturacion activa.
- API Key de OpenAI.
- Modelo a utilizar.
- Prompt o reglas de conversacion aprobadas por el cliente.
- ID del Prompt Template de OpenAI.
- Version del Prompt Template, si se quiere fijar una version especifica.

El prompt define el comportamiento conversacional: que puede responder, que debe evitar, como deriva, que tono usa y que formato interno debe devolver.

### Datos para Google Calendar

Solo aplica si se quiere que el bot consulte disponibilidad real:

- Cuenta de Google o Google Workspace donde este la agenda.
- Calendario que representa la agenda de turnos.
- Calendar ID de ese calendario.
- Proyecto de Google Cloud con Google Calendar API habilitada.
- Service Account de Google.
- Archivo JSON de la Service Account, preferentemente convertido a Base64.
- Permiso de lectura para la Service Account sobre el calendario.
- Dias y horarios en los que se pueden ofrecer turnos.
- Duracion estimada de los turnos.
- Anticipacion minima para ofrecer un horario.
- Cantidad de dias hacia adelante que se revisan.

### Datos para despliegue y operacion

- Plataforma donde se alojara el servicio, por ejemplo Railway, Render u otra.
- Base de datos PostgreSQL.
- Dominio o URL publica HTTPS para recibir webhooks de WhatsApp.
- Acceso al repositorio o mecanismo de deploy.
- Credenciales de GitHub Packages si el build necesita descargar dependencias privadas.
- Responsable operativo del lado del cliente.
- Telefonos de prueba.
- Fecha estimada de salida a produccion.

## 7. Configuraciones principales

Estas son las configuraciones de negocio que se definen antes de activar el servicio:

| Configuracion | Para que sirve |
| --- | --- |
| `BEAUTY_BOT_CLINIC_NAME` | Nombre de la clinica que usa el bot. |
| `BEAUTY_BOT_LOCATION` | Direccion o sede informada al cliente. |
| `BEAUTY_BOT_OPENING_HOURS` | Horarios de atencion que responde el bot. |
| `BEAUTY_BOT_ADVISOR_NOTIFICATION_PHONE_NUMBER` | Numero interno que recibe avisos de nuevos leads. |
| `BEAUTY_BOT_CAN_CHECK_AVAILABILITY` | Define si el bot puede consultar disponibilidad. |
| `BEAUTY_BOT_CALENDAR_WORKING_DAYS` | Dias habilitados para sugerir horarios. |
| `BEAUTY_BOT_CALENDAR_WORKING_START` | Hora de inicio de atencion. |
| `BEAUTY_BOT_CALENDAR_WORKING_END` | Hora de fin de atencion. |
| `BEAUTY_BOT_CALENDAR_SLOT_DURATION_MINUTES` | Duracion de cada bloque ofrecido. |
| `BEAUTY_BOT_CALENDAR_LOOKAHEAD_DAYS` | Cuantos dias hacia adelante revisa la agenda. |
| `BEAUTY_BOT_CALENDAR_MINIMUM_NOTICE_MINUTES` | Anticipacion minima para ofrecer un turno. |

Configuraciones operativas del servicio:

| Configuracion | Para que sirve |
| --- | --- |
| `PORT` | Puerto donde corre la aplicacion en el hosting. |
| `SPRING_DATASOURCE_URL` | Conexion a la base de datos PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos. |
| `SPRING_FLYWAY_ENABLED` | Habilita migraciones automaticas de base de datos. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Controla validacion/actualizacion del esquema de base de datos. |
| `BEAUTY_BOT_TEST_ENDPOINTS_ENABLED` | Habilita endpoints de prueba; en produccion debe quedar apagado salvo necesidad puntual. |
| `BEAUTY_BOT_INTERNAL_API_KEY_ENABLED` | Obliga a usar una clave interna para APIs privadas. |
| `BEAUTY_BOT_INTERNAL_API_KEY_HEADER` | Nombre del encabezado donde viaja la clave interna. |
| `BEAUTY_BOT_MASK_PHONE_NUMBERS` | Enmascara telefonos en logs para proteger datos personales. |

Configuraciones de IA:

| Configuracion | Para que sirve |
| --- | --- |
| `BEAUTY_BOT_AI_ENABLED` | Activa o desactiva el uso general de OpenAI. |
| `BEAUTY_BOT_AI_DECISION_ENABLED` | Permite que OpenAI decida la respuesta y el estado de la conversacion. |
| `BEAUTY_BOT_AI_DECISION_FALLBACK_ENABLED` | Si OpenAI falla temporalmente, permite usar el flujo local por reglas. |
| `BEAUTY_BOT_AI_MODEL` | Modelo de OpenAI configurado para la decision conversacional. |
| `BEAUTY_BOT_AI_TIMEOUT_SECONDS` | Tiempo maximo de espera para llamadas a OpenAI. |
| `BEAUTY_BOT_AI_DECISION_PROMPT_ID` | ID del Prompt Template aprobado. |
| `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION` | Version fija del prompt, si se quiere controlar cambios. |
| `BEAUTY_BOT_AI_PROMPT_CACHE_RETENTION` | Configuracion opcional de cache de prompt. |

Configuraciones de WhatsApp:

| Configuracion | Para que sirve |
| --- | --- |
| `WHATSAPP_ENABLED` | Activa o desactiva la integracion con WhatsApp. |
| `WHATSAPP_PHONE_NUMBER_ID` | Identifica el numero dentro de Meta. |
| `WHATSAPP_VERIFY_TOKEN` | Verifica el webhook cuando se conecta Meta con el servicio. |
| `WHATSAPP_ACCESS_TOKEN` | Autoriza el envio de mensajes por WhatsApp Cloud API. |
| `WHATSAPP_APP_SECRET` | Valida que los eventos recibidos vengan de Meta. |
| `WHATSAPP_BASE_URL` | URL base de la API de Meta/WhatsApp. |
| `BEAUTY_BOT_ADVISOR_NOTIFICATION_ENABLED` | Activa o desactiva avisos a asesora. |

Configuraciones de Google Calendar:

| Configuracion | Para que sirve |
| --- | --- |
| `BEAUTY_BOT_CALENDAR_ENABLED` | Activa o desactiva la integracion de agenda. |
| `BEAUTY_BOT_CALENDAR_TIME_ZONE` | Zona horaria usada para calcular horarios. |
| `GOOGLE_CALENDAR_ID` | Calendario que se consulta. |
| `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` | Credencial recomendada para leer la agenda. |
| `GOOGLE_SERVICE_ACCOUNT_JSON` | Alternativa menos recomendada para cargar la credencial. |

Configuraciones sensibles que deben tratarse como secretas:

| Configuracion | Para que sirve |
| --- | --- |
| `OPENAI_API_KEY` | Permite usar OpenAI. |
| `WHATSAPP_ACCESS_TOKEN` | Permite enviar mensajes por WhatsApp Cloud API. |
| `WHATSAPP_APP_SECRET` | Permite validar que los webhooks vienen de Meta. |
| `WHATSAPP_VERIFY_TOKEN` | Permite verificar el webhook inicial con Meta. |
| `WHATSAPP_PHONE_NUMBER_ID` | Identifica el numero de WhatsApp dentro de Meta. |
| `GOOGLE_SERVICE_ACCOUNT_JSON_BASE64` | Credencial para leer Google Calendar. |
| `BEAUTY_BOT_INTERNAL_API_KEY` | Protege endpoints internos del servicio. |
| `SPRING_DATASOURCE_PASSWORD` | Password de la base de datos. |
| `GITHUB_TOKEN` | Se usa para descargar dependencias privadas durante el build. |

## 8. Seguridad y privacidad

El servicio trabaja con datos personales y comerciales. Como minimo, se guardan:

- Numero de telefono.
- Mensajes de la conversacion.
- Nombre, si el cliente lo informa.
- Tratamiento de interes.
- Preferencias de horario.
- Estado del lead.

Buenas practicas recomendadas:

- No enviar credenciales por canales inseguros.
- Guardar claves como variables secretas del entorno de despliegue.
- Rotar claves si fueron compartidas por error.
- Mantener enmascaramiento de telefonos activo en logs de produccion.
- Limitar el acceso a endpoints internos con API Key.
- Definir quien del equipo puede ver leads y conversaciones.

## 9. Costos externos a considerar

El microservicio puede generar costos en servicios de terceros:

- OpenAI: consumo por uso de IA.
- Meta/WhatsApp: costos propios de mensajeria o conversaciones, segun la cuenta de WhatsApp Business.
- Hosting: costo de la plataforma donde corre el servicio.
- Base de datos PostgreSQL: costo del proveedor elegido.
- Google Cloud: normalmente se usa para la integracion de calendario; puede tener costos segun uso y configuracion de la cuenta.

Estos costos no dependen del codigo del microservicio, sino de las cuentas y planes contratados por el cliente.

## 10. Pruebas antes de salir a produccion

Antes de publicar el numero para clientes reales, conviene validar:

- Que el webhook de WhatsApp recibe mensajes correctamente.
- Que el bot responde desde el numero correcto.
- Que OpenAI responde con el tono y reglas aprobadas.
- Que si OpenAI falla temporalmente, el servicio puede usar el flujo de respaldo configurado.
- Que la agenda muestra disponibilidad real.
- Que no se ofrecen horarios fuera del rango configurado.
- Que se deriva a asesora ante consultas medicas, quejas, cancelaciones o reprogramaciones.
- Que la asesora recibe notificaciones internas.
- Que los leads se guardan con datos correctos.
- Que el bot deja de responder cuando la conversacion esta en manos de una asesora.
- Que los endpoints internos estan protegidos.

## 11. Responsabilidades del cliente

Para que el servicio funcione correctamente, el cliente debe:

- Proveer credenciales y accesos vigentes.
- Mantener activa la facturacion de OpenAI si se usa IA.
- Mantener activo el numero y la configuracion de WhatsApp Business.
- Mantener actualizada la agenda de Google Calendar si se usa disponibilidad real.
- Definir quien responde los leads derivados.
- Revisar y aprobar el contenido del prompt conversacional.
- Informar cambios de horarios, direccion, tratamientos o politicas comerciales.
- Avisar si cambia el numero interno de asesora.

## 12. Responsabilidades del proveedor tecnico

El proveedor tecnico debe:

- Configurar variables de entorno.
- Desplegar el servicio.
- Conectar WhatsApp Cloud API.
- Conectar OpenAI.
- Conectar Google Calendar si aplica.
- Proteger endpoints internos.
- Validar pruebas de punta a punta.
- Documentar credenciales requeridas sin exponerlas en el codigo.
- Ajustar prompt y reglas de conversacion cuando el cliente lo apruebe.

## 13. Checklist de entrega del cliente

Para avanzar sin bloqueos, el cliente deberia entregar o confirmar:

- Nombre de la clinica.
- Direccion.
- Horarios de atencion.
- Tratamientos principales.
- Politica de precios y promociones.
- Numero de WhatsApp que usara el bot.
- Acceso o datos de Meta Business.
- Phone Number ID.
- Access Token de WhatsApp.
- App Secret de Meta.
- Verify Token acordado.
- Numero de asesora para notificaciones.
- Cuenta de OpenAI Platform.
- API Key de OpenAI.
- Prompt aprobado o reglas de conversacion.
- Google Calendar ID, si se usara agenda.
- Credencial de Service Account de Google, si se usara agenda.
- Dias y horarios reales para ofrecer disponibilidad.
- Duracion estimada de turnos.
- Telefonos de prueba.
- Responsable operativo para validar la salida a produccion.

## 14. Recomendacion de salida a produccion

La salida a produccion deberia hacerse en dos etapas:

1. Prueba controlada con telefonos internos, validando conversaciones reales sin exponer el numero masivamente.
2. Activacion gradual con clientes reales, monitoreando respuestas, derivaciones, leads creados y funcionamiento de agenda.

Durante los primeros dias conviene revisar conversaciones manualmente para ajustar el prompt, frases frecuentes y criterios de derivacion.
