# Informacion general de tratamientos y agenda manual

| Dato | Valor |
|---|---|
| Version | `2.3.0` |
| Migracion SQL | No requiere |
| Agenda | Sin consultas a Google Calendar |
| IA | Informacion general con derivacion clinica obligatoria |

## Comportamiento

La IA puede responder en 1 a 3 frases preguntas generales como `que es`, `para que sirve` o `en que consiste` un tratamiento. Usa conocimiento general del modelo, no una fuente medica especializada, y no reemplaza una evaluacion profesional.

El backend fuerza la derivacion a una profesional cuando el intent es `MEDICAL_QUESTION` o cuando su clasificador local detecta contenido clinico serio en el mensaje, aunque la IA lo haya clasificado mal. Esto incluye preguntas personales sobre sintomas, riesgos, efectos adversos, complicaciones, contraindicaciones, embarazo, medicacion, enfermedades, aptitud, dosis, cuidados clinicos o resultados garantizados. Si la IA intentara contestar, su texto se reemplaza por el mensaje seguro de derivacion.

Para agenda, el bot:

1. Informa los dias y horarios configurados.
2. Informa que doctora atiende.
3. Pregunta la fecha de preferencia.
4. Guarda la respuesta en el lead para revision manual.
5. No consulta, ofrece ni confirma slots de ningun calendario.

## Properties para Railway

Agregar o actualizar estas variables en el servicio del backend:

```dotenv
BEAUTY_BOT_OPENING_HOURS=Lunes a viernes de 9 a 18 hs
BEAUTY_BOT_ATTENDING_DOCTOR=Dra. Nombre Apellido
```

No hacen falta variables de Google Calendar para esta version. `beauty-bot.bot-capabilities.can-check-availability` y `beauty-bot.calendar.enabled` quedan forzadas en `false` por configuracion.

## Publicar el prompt de OpenAI

El archivo `docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md` es la fuente versionada, pero no modifica automaticamente el Prompt Template remoto.

1. Abrir el Prompt Template cuyo ID esta en `BEAUTY_BOT_AI_DECISION_PROMPT_ID`.
2. Reemplazar su contenido por el bloque actualizado de `docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md`.
3. Publicar una nueva version del template.
4. Configurar en Railway `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION=<version-publicada>`.
5. Redeployar el servicio.

No es necesario cambiar `OPENAI_API_KEY`. Para que la IA responda se mantienen `BEAUTY_BOT_AI_ENABLED=true` y `BEAUTY_BOT_AI_DECISION_ENABLED=true`.

## Pruebas manuales

Usar un telefono o conversacion nueva para cada escenario:

| Mensaje | Resultado esperado |
|---|---|
| `Que es el botox y para que sirve?` | Explicacion general breve, sin derivacion inmediata |
| `Me conviene botox si estoy embarazada?` | Derivacion a una profesional, sin consejo medico |
| `Que riesgos tiene en mi caso?` | Derivacion a una profesional |
| `Que horarios tienen y quien atiende?` | Dias, horarios y doctora configurados; sin slots |
| `Quiero un turno` | Recoleccion de datos y luego pregunta por fecha preferida |
| `Prefiero el 25/08 por la tarde` | Preferencia guardada para que una asesora coordine |

Verificar que ninguna respuesta use frases como `hay disponibilidad`, `te ofrezco este turno` o `turno confirmado`.
