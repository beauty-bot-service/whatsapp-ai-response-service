# Entrega única de promociones por conversación

| Dato | Valor |
|---|---|
| Versión | `2.2.0` |
| Migración | `V10` |
| Regla | Cada promoción se envía como máximo una vez por conversación |

## Comportamiento

La primera mención de una promoción activa incorpora su cuerpo canónico en la respuesta y registra la entrega en `CONVERSATION_PROMOTION_DELIVERIES`. Las siguientes menciones de la misma promoción dentro de la misma `CONVERSATION_SESSION` no vuelven a incorporar ese cuerpo; el bot conserva la respuesta conversacional normal.

Si un mensaje coincide con varias promociones, sólo se agregan las que todavía no fueron enviadas. Una sesión nueva permite volver a entregar las promociones.

El registro y el mensaje saliente se guardan dentro de la misma transacción. Una restricción única sobre `CONVERSATION_SESSION_ID` y `PROMOTION_ID`, junto con el bloqueo por teléfono, evita duplicados concurrentes.

## Prueba manual

1. Crear y activar `botox-qa` con alias `botox qa` y cuerpo `PROMO_BOTOX_QA`.
2. Usar un teléfono nuevo y enviar `Quiero botox qa`.
3. Confirmar que la respuesta contiene `PROMO_BOTOX_QA`.
4. Con el mismo teléfono enviar `¿Y cuál es el precio del botox qa?`.
5. Confirmar que la segunda respuesta no contiene `PROMO_BOTOX_QA` y sí conserva la respuesta normal para precio.
6. Usar otro teléfono y confirmar que la promoción vuelve a enviarse una vez.

## Evidencia SQL

```sql
SELECT conversation_session_id, promotion_id, promotion_code, delivered_at
FROM conversation_promotion_deliveries
ORDER BY delivered_at DESC;
```

No debe haber más de una fila para la misma combinación de conversación y promoción.
