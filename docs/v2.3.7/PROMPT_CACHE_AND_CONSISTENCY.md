# Cache y consistencia del prompt

La version `2.3.7` maximiza Prompt Caching sin compartir datos entre conversaciones:

- la clave deja de depender del telefono y se basa en el ID y la version del prompt;
- la retencion predeterminada sigue siendo `24h`;
- el contexto dinamico permanece al final del prompt y separado por solicitud;
- el payload ahora incluye las sugerencias y el estado de consulta de disponibilidad;
- el template elimina reglas duplicadas, conserva el contrato JSON y corrige los ejemplos con emojis.

Prompt Caching no garantiza un acierto en todos los pedidos. Para mantener estabilidad y una alta tasa de aciertos, fijar `BEAUTY_BOT_AI_DECISION_PROMPT_VERSION` y cambiarla al publicar una nueva revision del prompt.
