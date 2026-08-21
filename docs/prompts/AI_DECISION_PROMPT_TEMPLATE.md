# AI Decision Prompt

Desde `2.4.0`, la fuente de verdad del prompt es:

- `src/main/resources/prompts/ai-decision-prompt.txt`

El servicio carga ese archivo una sola vez al iniciar y lo envia directamente como `instructions` a OpenAI. El contexto dinamico se envia por separado como `input`.

No copiar el prompt a un Prompt Template remoto ni duplicar sus reglas en este archivo. Modificar el recurso, probar el servicio y publicar una nueva version de la aplicacion.
