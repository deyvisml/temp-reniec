## Why

La consulta inicial ya puede habilitar el paso de identidad, pero el proyecto todavía no dispone de una autenticación real con ID Perú ni de una autorización temporal que proteja los pasos posteriores. Incorporar ahora el flujo oficial OAuth 2.0/OpenID Connect de ID Perú v1.2 evita que el `requestId` se trate como autorización y deja una frontera de seguridad comprobable antes de consultar o seleccionar certificados individuales.

## What Changes

- Incorporar como referencia permanente el PDF aprobado **IDAAS V2 – Especificaciones Técnicas**, versión 1.2 del 22/05/2026, acompañado por una guía que delimite configuración institucional y tratamiento de credenciales.
- Implementar el paso 1 de cinco con una vista responsive basada en `docs/ui-reference/step-1.png`, estados accesibles de inicio, retorno, procesamiento, éxito, cancelación, rechazo, expiración e indisponibilidad.
- Añadir un flujo backend de ID Perú v2 con OAuth 2.0, OpenID Connect, Authorization Code, PKCE `S256`, `state` aleatorio de un solo uso, callback `POST`, intercambio de código, validación de `id_token`, consulta de `/userinfo` y validación del JWT retornado mediante JWKS.
- Cifrar el DNI exclusivamente para el parámetro `vd` con AES/CBC/PKCS5Padding conforme al documento técnico, sin exponer DNI, códigos, verificadores ni tokens al frontend o a los logs.
- Separar la orquestación mediante un puerto de identidad y dos adaptadores intercambiables: ID Perú real y simulador determinista para desarrollo y pruebas.
- Verificar en backend que el DNI autenticado coincide con el de la solicitud antes de marcar la identidad como verificada o habilitar el paso 2.
- Ampliar incrementalmente `identity_verification` para controlar modo, `state`, PKCE, expiración, consumo, resultado y autorización temporal, sin crear una tabla de sesiones ni recuperar trámites anteriores.
- Emitir tras una autenticación válida una autorización corta en cookie `HttpOnly`, validada contra el intento persistido, sin DNI ni datos personales, e invalidable al finalizar, abandonar o expirar el flujo.
- **BREAKING**: exigir la autorización temporal para toda API posterior a la identidad y no considerar `requestId` suficiente para acceder al paso 2.
- Documentar y probar configuración, OpenAPI, callback, errores, criptografía, modo simulado y modo real contra un servidor controlado; la prueba con ID Perú institucional quedará sujeta a credenciales y convenio autorizados.

## Capabilities

### New Capabilities

- `id-peru-citizen-authentication`: inicio, retorno, validación OAuth/OIDC, PKCE, `vd`, JWKS, `/userinfo`, coincidencia de DNI, adaptadores real/simulado, interfaz del paso 1 y errores controlados.
- `temporary-flow-authorization`: cookie de corta duración vinculada a la solicitud y al intento verificado, protección del paso 2 e invalidación sin sesión permanente ni recuperación de trámites.

### Modified Capabilities

- `citizen-eligibility-entry`: una disponibilidad positiva solo permite iniciar ID Perú; el `requestId` no autoriza el acceso al paso 2.
- `cancellation-request-persistence-model`: los intentos de identidad almacenan el material temporal mínimo y el estado de la autorización, sin tokens ni una tabla de sesiones.
- `frontend-backend-integration`: se añaden los contratos versionados de inicio, callback, estado y salida de ID Perú, cookies con credenciales y errores normalizados.
- `backend-foundation`: se incorpora validación JWT/JWKS, configuración externa fail-closed y protección selectiva de endpoints posteriores sin convertir el backend en un servidor OAuth.
- `project-reference-materials`: el PDF ID Perú v1.2 y su README pasan a ser referencias permanentes obligatorias para autenticación y seguridad relacionada.

## Impact

- Backend: nuevo módulo de integración ID Perú, configuración, endpoints, seguridad de cookie, migración Flyway incremental, entidades/repositorios, OpenAPI y pruebas.
- Frontend: nueva ruta de verificación de identidad, redirección controlada, procesamiento del retorno, cliente HTTP con cookies y pruebas responsive/accesibles.
- Dependencias: una biblioteca JOSE/JWT mantenida y compatible con Spring Boot para validación criptográfica; no se incorporará un SDK OAuth cliente completo si las primitivas nativas cubren el contrato.
- Operación: salidas HTTPS hacia `/auth`, `/token`, `/userinfo`, `/jwks` y opcionalmente `/logout`; secretos y URLs por ambiente, con integración real deshabilitada si falta configuración obligatoria.
- Datos: cambio incremental sobre `identity_verification`; sin nueva tabla de sesión, sin persistencia de tokens, biometría, fotografías o payloads completos.
