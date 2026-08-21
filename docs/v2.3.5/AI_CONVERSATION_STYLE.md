# Estilo conversacional y consultas basicas

La version `2.3.5` actualiza el contrato conversacional de IA:

- permite un emoji simple y pertinente en las respuestas;
- usa ejemplos breves de conversacion como referencia de tono;
- conserva los mensajes recientes reales para mantener continuidad;
- permite informacion general y educativa sobre tratamientos;
- clasifica preguntas sobre en que consiste, como se realiza, para que sirve o cuanto demora como informacion general;
- mantiene la derivacion obligatoria para consultas personalizadas o sensibles;
- pide resumenes humanos naturales y accionables, sin repetir el telefono.

Para activar el cambio en produccion, crear una nueva version del prompt de decision con el contenido de `docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md` y configurar esa version mediante `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION`.
