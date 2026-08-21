# Prompt de decision local

La version `2.4.0` elimina la dependencia de Prompt Templates remotos de OpenAI.

- La fuente de verdad es `src/main/resources/prompts/ai-decision-prompt.txt`.
- El recurso se carga y valida una sola vez durante el arranque de Spring.
- Las reglas se envian como `instructions` y el contexto de cada conversacion como `input`.
- La clave de Prompt Caching se calcula con una huella del contenido; cambiar el archivo renueva la clave automaticamente.
- `BEAUTY_BOT_AI_DECISION_PROMPT_ID` y `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION` dejan de utilizarse.
- `BEAUTY_BOT_AI_DECISION_PROMPT_RESOURCE` permite reemplazar la ubicacion del recurso, aunque normalmente debe conservarse el valor predeterminado.

El prompt queda versionado, revisable e inmutable durante la vida de cada instancia. Una nueva version entra en vigencia al desplegar y reiniciar el servicio.
