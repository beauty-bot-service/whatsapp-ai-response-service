# Correccion de listado de promociones en PostgreSQL

| Dato | Valor |
|---|---|
| Version | `2.3.2` |
| Migracion SQL | No requiere |
| Sintoma | El panel mostraba `Internal Server Error` despues de crear o publicar |

La consulta administrativa combinaba filtros opcionales nulos en una unica expresion JPQL. Esa forma era aceptada por H2, pero podia fallar al bindear parametros sin tipo en PostgreSQL.

El backend ahora selecciona una consulta sin parametros nulos para cada combinacion:

1. Todas las promociones.
2. Promociones filtradas por estado.
3. Busqueda por titulo o codigo.
4. Busqueda por titulo o codigo y estado.

La prueba de integracion reproduce el request exacto del frontend con `status=ACTIVE`, paginado y orden por `updatedAt`.

## Verificacion en Railway

1. Desplegar `2.3.2`.
2. Abrir el panel y seleccionar `Publicadas`.
3. Confirmar que la promocion activa aparece sin `Internal Server Error`.
4. Probar `Me interesa botox` en el laboratorio de matching.
5. Usar una conversacion nueva para comprobar el envio, porque cada promocion se entrega como maximo una vez por conversacion.
