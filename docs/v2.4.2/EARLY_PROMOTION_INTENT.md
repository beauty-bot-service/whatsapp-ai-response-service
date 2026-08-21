# Intencion promocional en primeros mensajes

La version `2.4.2` adapta la interpretacion al origen habitual de los contactos desde redes sociales.

- En los primeros mensajes, frases amplias como "quiero averiguar", "quiero informacion" o "vi el tratamiento en redes" activan todas las promociones relacionadas.
- Las preguntas explicitas sobre que es, como se realiza, para que sirve o cuanto demora conservan prioridad informativa y no activan promociones.
- Despues de enviar una promocion, un pedido de mas informacion se interpreta como consulta sobre el procedimiento.
- `PromotionDeliveryRegistryService` mantiene el bloqueo deterministico de promociones ya enviadas por sesion.
