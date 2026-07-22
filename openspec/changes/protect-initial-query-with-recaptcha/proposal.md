## Why

La consulta inicial es pública y actualmente permite invocar el servicio de disponibilidad únicamente con un DNI válido, por lo que necesita una barrera anti-automatización antes de crear solicitudes o consumir la integración protegida. El proyecto institucional de referencia confirma el uso de Google reCAPTCHA v2 Checkbox y la verificación server-to-server, modalidad que se adoptará sin copiar su complejidad adicional ni sus credenciales dentro del repositorio.

## What Changes

- Integrar Google reCAPTCHA v2 Checkbox en el formulario real de inicio mediante una librería mantenida y una site key pública configurada en el build del frontend.
- **BREAKING**: ampliar `POST /api/v1/cancellation-requests` para exigir `recaptchaToken` junto con el DNI y regenerar OpenAPI y los tipos TypeScript.
- Verificar el token en el backend antes de preparar o persistir una solicitud y antes de invocar `CertificateAvailabilityPort`.
- Incorporar un puerto anti-bot y un adaptador Google `siteverify` con timeout, allowlist de hostnames, validación defensiva de respuesta y errores públicos diferenciados.
- Reiniciar y descartar el token después de cada intento, expiración, rechazo o error, sin almacenarlo en URL, logs, cookies ni almacenamiento del navegador.
- Configurar site key, secret, endpoint, timeout y hostnames por ambiente; las claves oficiales de prueba identificadas en el proyecto de referencia se usarán solo mediante archivos locales ignorados para la comprobación manual.
- Añadir pruebas aisladas de frontend, backend e integración que demuestren que una evidencia inválida nunca crea una solicitud ni consulta disponibilidad.
- Actualizar documentación operativa, OpenAPI y decisiones técnicas sin incorporar autenticación, rate limiting, fingerprinting ni funcionalidades posteriores del flujo.

## Capabilities

### New Capabilities
- `initial-query-recaptcha-protection`: renderizado, ciclo de vida, verificación backend, configuración, privacidad, errores y pruebas de Google reCAPTCHA v2 Checkbox para la consulta pública inicial.

### Modified Capabilities
- `citizen-eligibility-entry`: la iniciación exige evidencia reCAPTCHA válida antes de crear la solicitud o consultar disponibilidad.
- `frontend-backend-integration`: el contrato compartido añade `recaptchaToken` y errores anti-bot sincronizados mediante OpenAPI.
- `frontend-foundation`: se permite la dependencia mantenida y la variable pública estrictamente necesarias para el widget v2 Checkbox.
- `backend-foundation`: se permite la integración externa anti-bot, su configuración segura y sus pruebas aisladas dentro del monolito.

## Impact

- Frontend: formulario de DNI, componente reCAPTCHA, variables de entorno, manejo de errores, pruebas y dependencia npm específica.
- Backend: DTO y endpoint de inicio, orquestación, puerto/adaptador Google, configuración, errores comunes, OpenAPI y pruebas.
- Contrato: cambio incompatible del cuerpo de la solicitud inicial; la respuesta de disponibilidad de SPEC-10 permanece sin cambios.
- Operación: comunicación saliente HTTPS con Google y configuración local/productiva externa; sin migraciones ni cambios en las siete tablas actuales.
