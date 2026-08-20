# Destinatarios argentinos en WhatsApp Cloud API

La version `2.3.4` normaliza los destinatarios argentinos antes de enviar mensajes a Meta.

En webhooks, WhatsApp identifica celulares argentinos como `549` seguido por los diez digitos nacionales. En el entorno de prueba, Meta registra el destinatario permitido sin el `9` movil y compara ese valor literalmente. Por eso, un remitente recibido como `549XXXXXXXXXX` se envia como `54XXXXXXXXXX`.

La normalizacion solo se aplica al payload saliente. El numero original de la conversacion no se modifica y los numeros de otros paises permanecen sin cambios.
