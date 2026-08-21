# Intencion promocional

La version `2.4.1` evita que una coincidencia literal con el nombre de un tratamiento reemplace una respuesta informativa por una promocion.

- Las decisiones de IA usan exclusivamente los codigos seleccionados semanticamente en `matchedPromotionCodes`.
- El matcher literal queda limitado al fallback local con intencion `PRICE_QUESTION`.
- El prompt diferencia consultas generales de intenciones comerciales sobre precios, promociones, descuentos, cuotas y ofertas.
