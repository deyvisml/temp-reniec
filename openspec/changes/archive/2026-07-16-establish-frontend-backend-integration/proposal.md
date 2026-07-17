## Why

Frontend y backend ya pueden ejecutarse por separado, pero todavía no comparten una API versionada, documentación de contrato, tipos generados ni una comprobación real que atraviese las tres capas. Completar esta base ahora evitará que las siguientes funcionalidades repitan configuración de URL, CORS, errores, correlación, timeout y contratos.

## What Changes

- Establecer `/api/v1` como prefijo de las APIs de aplicación y añadir `GET /api/v1/system/status` como único endpoint técnico de esta tarea.
- Hacer que el endpoint de estado ejecute una comprobación real y ligera contra MySQL, devuelva únicamente información técnica saneada y use el formato común de error ante indisponibilidad.
- Configurar CORS mediante una lista explícita de orígenes permitidos, con credenciales futuras, headers mínimos y exposición de `X-Correlation-ID`, sin comodines.
- Ampliar el cliente HTTP nativo del frontend para URL pública/servidor, generación y propagación de correlación, timeout, JSON, errores HTTP, respuestas inválidas y errores de red, sin reintentos ni interceptores.
- Documentar la API con OpenAPI generado desde el backend y limitar el documento a `/api/v1/**`.
- Generar y versionar tipos TypeScript desde el OpenAPI real, incluyendo una comprobación de desalineación del contrato.
- Sustituir el estado de integración pendiente de la página temporal por una comprobación visible, accesible y no funcional del backend y MySQL.
- Añadir pruebas backend de endpoint, base de datos, errores, correlación, CORS y OpenAPI; mantener las pruebas frontend unitarias aisladas y añadir una suite separada que use el cliente real contra el stack local.
- Documentar un recorrido reproducible para iniciar MySQL, backend y frontend, sincronizar contratos y ejecutar las verificaciones.
- Mantener fuera de alcance DNI, solicitudes de cancelación, sesión JWT, recuperación, ID Perú, motivos, confirmación, revocación, constancia y administración.

## Capabilities

### New Capabilities

- `frontend-backend-integration`: Contrato API versionado, CORS, estado técnico con MySQL, OpenAPI, tipos TypeScript, cliente HTTP, indicador temporal y pruebas reales entre aplicaciones.

### Modified Capabilities

- `backend-foundation`: La base backend admitirá la dependencia OpenAPI necesaria y un endpoint técnico versionado, manteniendo sus garantías de error, correlación, salud y alcance no funcional.
- `frontend-foundation`: La base frontend pasará de una integración reservada a una integración técnica real, con URL pública segura, tipos generados, cliente con timeout/correlación y estado temporal visible.

## Impact

El backend incorporará un pequeño módulo técnico de estado, configuración CORS, documentación OpenAPI y pruebas con el MySQL Testcontainer existente. El frontend modificará variables de entorno, cliente HTTP, página temporal, tipos generados, scripts de sincronización y pruebas. Se añadirán únicamente `springdoc-openapi-starter-webmvc-api` en backend y `openapi-typescript` como dependencia de desarrollo en frontend; no se añade librería HTTP, SDK, estado global, autenticación ni lógica del flujo ciudadano.
