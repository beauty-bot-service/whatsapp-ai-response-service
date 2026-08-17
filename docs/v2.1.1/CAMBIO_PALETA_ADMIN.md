# Cambio de paleta del panel administrativo

| Dato | Valor |
|---|---|
| Versión | `2.1.1` |
| Alcance | Presentación visual de `admin-web` |

El panel adopta una paleta inspirada en la identidad visual de Doctor Beauty: fondos marfil y blanco cálido, texto carbón, dorado como color principal y beige para superficies secundarias.

No se modificaron rutas, contratos de API, autenticación, persistencia ni comportamiento de promociones. Los colores verdes y rojos se conservan únicamente cuando comunican estados funcionales o errores.

## Verificación

```powershell
cd admin-web
npm run build
```

Revisar login, catálogo, editor, vista previa y diseño responsive en desktop y mobile.
