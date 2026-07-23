## Context

SPEC-10 dejó `POST /api/v1/cancellation-requests` como una consulta pública de existencia: valida el DNI, crea una solicitud, registra el intento y recién entonces invoca el puerto de disponibilidad. Al no existir todavía autenticación, cualquier cliente puede automatizar esa operación y multiplicar escrituras y llamadas al proveedor.

El proyecto de referencia `C:\FastFolder\sistema-autorizacion-certificados-reniec` confirma Google reCAPTCHA v2 Checkbox. Su frontend usa `@google-recaptcha/react` 2.4.2, reinicia el widget mediante una clave de montaje y envía el token en el JSON. Su backend usa un `RecaptchaVerifier` separado, `RestClient`, `application/x-www-form-urlencoded`, timeouts y el endpoint oficial `siteverify`; las pruebas automatizadas sustituyen Google y el ambiente de desarrollo usa el par oficial de claves de prueba publicado por Google. No se copiarán su estado global, React Query, circuit breaker ni configuración con secretos versionados. Esta adaptación añadirá además la validación de hostname que la referencia no realiza.

## Goals / Non-Goals

**Goals:**

- Exigir una evidencia reCAPTCHA v2 Checkbox válida antes de crear solicitudes o consultar certificados.
- Mantener detalles HTTP de Google fuera del caso de uso mediante un puerto anti-bot.
- Gestionar token, expiración, reinicio, carga, errores y accesibilidad en el formulario existente.
- Externalizar site key, secret, timeout y hostnames, sin registrar ni persistir evidencia.
- Mantener intactas las respuestas funcionales y la persistencia de disponibilidad de SPEC-10.

**Non-Goals:**

- Implementar reCAPTCHA v3, Enterprise, otro CAPTCHA, rate limiting distribuido, bloqueo por IP o fingerprinting.
- Añadir circuit breaker, reintentos automáticos, una tabla CAPTCHA o auditoría de tokens.
- Implementar ID Perú, listado, selección, motivo, confirmación, revocación o constancia.

## Decisions

### 1. Google reCAPTCHA v2 Checkbox con una dependencia frontend acotada

Se usará `@google-recaptcha/react`, validando y fijando una versión compatible con Next.js 16 y React 19 durante la implementación. El componente será un wrapper pequeño controlado por el formulario; no se incorporarán React Query, React Hook Form, Zod ni estado global.

La site key se leerá de `NEXT_PUBLIC_RECAPTCHA_SITE_KEY`, que es deliberadamente pública. La ausencia de esta variable no romperá el build, pero el formulario mostrará una indisponibilidad controlada y no permitirá enviar.

**Alternativa descartada:** insertar manualmente el script global y gestionar `grecaptcha.render/reset`. Reduciría una dependencia, pero aumentaría código propio de carga, montaje concurrente y limpieza, precisamente los aspectos que se busca delegar a una integración mantenida.

### 2. El token es evidencia efímera de un solo intento

El formulario mantendrá el token solo en memoria. El botón estará habilitado únicamente con DNI válido, token presente y sin envío activo. Tras cualquier intento de red, éxito, rechazo, timeout, expiración o error del widget, se vaciará el token y se remontará/reiniciará el widget. Si Google rechaza el token desde el backend, el ciudadano deberá completar un desafío nuevo.

No habrá `NEXT_PUBLIC_RECAPTCHA_TEST_MODE` ni un token de bypass incluido en el bundle productivo. Las pruebas de frontend usarán dobles del wrapper o lógica de estado aislada.

### 3. El caso de uso verifica anti-bot antes de toda mutación

`StartCancellationRequest` incorporará `recaptchaToken` con límites de presencia y longitud. `CancellationRequestInitiationService` dependerá de `AntiBotVerificationPort` y ejecutará:

1. validación Jakarta del DTO;
2. `antiBotVerificationPort.verify(token)`;
3. `AvailabilityPersistenceCoordinator.prepare(...)`;
4. consulta y finalización de disponibilidad existentes.

Una excepción anti-bot ocurre antes de `prepare`, por lo que no existe solicitud, intento ni llamada a `CertificateAvailabilityPort`. La verificación de Google no estará dentro de una transacción de base de datos.

**Alternativa descartada:** crear la solicitud antes de validar y abandonarla al fallar. Generaría historial y escrituras provocadas por tráfico no validado, contradiciendo el propósito del control.

### 4. Adaptador Google defensivo con RestClient nativo de Spring

`GoogleRecaptchaVerificationAdapter` enviará `secret` y `response` como formulario a `https://www.google.com/recaptcha/api/siteverify`, usando timeouts positivos configurados y sin enviar `remoteip`, para no añadir tratamiento de IP del ciudadano. Se aceptará únicamente una respuesta JSON válida con `success=true`.

Si `hostname` viene informado, deberá pertenecer a `RECAPTCHA_ALLOWED_HOSTNAMES` mediante comparación exacta normalizada a minúsculas; no se permitirán sufijos parciales. Un hostname ausente no invalidará por sí solo una respuesta exitosa, conforme al requisito de validarlo cuando esté disponible. `timeout-or-duplicate` se mapeará conjuntamente porque Google no distingue expiración de reutilización.

Se limitarán longitud y caracteres de configuración y token, y nunca se incluirán token, secret, formulario externo ni respuesta completa en logs o errores.

**Alternativa descartada:** copiar el circuit breaker de la referencia. Un timeout corto y errores controlados cubren este incremento; el endpoint no realizará reintentos automáticos y una política de resiliencia compartida se justificará solo cuando existan más integraciones reales.

### 5. Errores anti-bot independientes de disponibilidad

El formato `ApiError` y la correlación existentes se conservarán. Los códigos públicos serán estables:

- `RECAPTCHA_REQUIRED` para evidencia ausente o vacía;
- `RECAPTCHA_REJECTED` para token inválido u hostname no permitido;
- `RECAPTCHA_EXPIRED_OR_DUPLICATE` para el código Google correspondiente;
- `RECAPTCHA_UNAVAILABLE` para conectividad o HTTP no satisfactorio;
- `RECAPTCHA_TIMEOUT` para tiempo agotado;
- `RECAPTCHA_INVALID_RESPONSE` para cuerpo nulo o malformado.

Los tres primeros serán rechazos de solicitud sin reintento automático; indisponibilidad, timeout y respuesta inválida se comunicarán como fallas temporales independientes de la ausencia de certificados. Los mensajes de frontend reiniciarán el widget y nunca afirmarán que el DNI no tiene certificados.

### 6. Configuración fail-closed y credenciales externas

El backend real requerirá `RECAPTCHA_SECRET_KEY` y `RECAPTCHA_ALLOWED_HOSTNAMES`; timeout y URI oficial tendrán valores seguros/documentados donde corresponda. La configuración inválida impedirá iniciar el adaptador real. El perfil de pruebas usará un doble determinista y no accederá a Google.

El frontend documentará `NEXT_PUBLIC_RECAPTCHA_SITE_KEY`. `.env.example` contendrá placeholders, no el secret ni credenciales productivas. La prueba manual colocará el par oficial de prueba de Google identificado en la referencia únicamente en `backend/.env` y `frontend/.env.local`, ambos ignorados. Producción deberá sustituirlo por claves propias restringidas al dominio institucional.

## Risks / Trade-offs

- **[Google o su script no está disponible]** → bloquear el envío, mostrar un mensaje accesible y permitir recargar/reintentar manualmente sin consultar disponibilidad.
- **[Token consumido pero respuesta al navegador perdida]** → reiniciar siempre el widget; nunca reutilizar ni reintentar automáticamente el token.
- **[Hostname productivo incompleto en la allowlist]** → documentar y validar explícitamente la lista por ambiente antes del despliegue.
- **[La clave pública queda visible]** → comportamiento esperado; solo el secret permanece en backend y fuera del repositorio.
- **[reCAPTCHA reduce pero no elimina abuso]** → mantener documentado que rate limiting perimetral/distribuido es una decisión posterior.
- **[Las claves oficiales de prueba aceptan cualquier desafío]** → limitar su uso a desarrollo manual y prohibirlas expresamente en producción.

## Migration Plan

1. Añadir configuración y adaptador backend con pruebas aisladas, manteniendo el mock de disponibilidad actual.
2. Ampliar DTO, servicio, errores y OpenAPI; verificar que todos los rechazos ocurran antes de persistencia.
3. Regenerar snapshot y tipos TypeScript.
4. Integrar el widget y ciclo de reinicio en el formulario; actualizar pruebas y documentación.
5. Configurar las claves oficiales de prueba solo en archivos locales ignorados y realizar la comprobación manual.
6. Para revertir, retirar conjuntamente el campo requerido, el paso de verificación y el widget; no existe migración de datos que deshacer.

## Open Questions

- Confirmar antes de producción los hostnames institucionales definitivos que se registrarán tanto en Google como en `RECAPTCHA_ALLOWED_HOSTNAMES`.
- Confirmar si la red productiva requerirá proxy o allowlisting de salida hacia `www.google.com`; no afecta la implementación local ni el contrato del puerto.
