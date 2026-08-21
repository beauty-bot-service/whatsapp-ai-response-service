# Interes promocional inicial deterministico

La version `2.4.3` agrega un respaldo deterministico para frases amplias de interes comercial que el modelo puede interpretar de forma variable.

- Reconoce familias como "quiero informacion", "quiero saber mas", "quiero averiguar", "me interesa" y referencias a redes sociales.
- Se aplica solamente dentro de los dos primeros mensajes del cliente.
- No busca promociones si detecta una pregunta explicita sobre que es, en que consiste, para que sirve, como funciona, como se realiza o cuanto demora.
- El texto normalizado se compara con el catalogo de aliases para determinar los tratamientos; una frase sin tratamiento coincidente no envia promociones.
- Las promociones ya entregadas siguen siendo eliminadas por `PromotionDeliveryRegistryService` antes de componer la respuesta.
