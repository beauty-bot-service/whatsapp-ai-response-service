# Prompt de decision consolidado

La version `2.3.6` restaura las reglas operativas que se habian perdido al resumir el prompt de decision, manteniendo las mejoras de tono de `2.3.5`.

Incluye:

- continuidad basada en mensajes recientes;
- interpretacion contextual de respuestas cortas;
- captura ordenada de datos del lead;
- reglas completas por intencion;
- separacion entre informacion general y consultas medicas personales;
- promociones administradas por el backend;
- emojis moderados y ejemplos anonimizados;
- resumen humano natural y accionable;
- esquema JSON sincronizado con `ConversationDecision`.

El prompt listo para produccion es el bloque `text` de `docs/prompts/AI_DECISION_PROMPT_TEMPLATE.md`.
