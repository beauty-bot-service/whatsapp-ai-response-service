# Logging de diagnostico de WhatsApp

La version `2.3.3` agrega logs para seguir el flujo completo de WhatsApp:

- recepcion del webhook y presencia de la firma;
- recepcion de cada mensaje con `messageId` y telefono enmascarado;
- inicio y aceptacion del envio hacia Meta;
- estado HTTP y cuerpo de error devuelto por Meta cuando falla un envio;
- rechazo critico de firma y errores no controlados del procesamiento asincrono.

## Payloads completos

Los JSON completos estan deshabilitados por defecto porque contienen telefonos, nombres y contenido potencialmente sensible.
Para una sesion breve de diagnostico en Railway:

```dotenv
BEAUTY_BOT_WHATSAPP_LOG_PAYLOADS=true
```

Luego de reproducir el problema, volver a `false` y desplegar nuevamente. Esta opcion registra el JSON entrante de Meta y el JSON saliente enviado a Cloud API; nunca registra el access token ni el App Secret.
