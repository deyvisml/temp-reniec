## Why

El backend ya genera un contrato OpenAPI parcial para sus rutas versionadas, pero no ofrece Swagger UI y la cobertura documental depende de anotaciones aisladas sin una verificación integral. Se necesita convertir esa base en documentación navegable, comprobable y obligatoria para que los consumidores comprendan la API real y los futuros cambios no introduzcan contratos desactualizados.

## What Changes

- Sustituir el starter API-only de springdoc por `springdoc-openapi-starter-webmvc-ui` 3.0.3, compatible con Spring Boot 4.1.0, sin incorporar otra biblioteca de documentación.
- Habilitar `/v3/api-docs`, `/v3/api-docs.yaml` y Swagger UI únicamente en el perfil local de desarrollo; mantener su exposición deshabilitada por defecto y sin definir aún configuración productiva.
- Completar la información general del contrato OpenAPI y organizar las operaciones existentes mediante etiquetas funcionales claras.
- Documentar exhaustivamente `GET /api/v1/system/status`, `POST /api/v1/cancellation-requests` y el único endpoint Actuator expuesto, `GET /actuator/health`, distinguiendo la API de aplicación de la operación técnica.
- Describir propósito, parámetros, cabeceras, cuerpos, formatos, validaciones, respuestas exitosas, errores controlados y códigos HTTP reales de cada operación.
- Completar los esquemas de los DTO actuales, incluidos `StartCancellationRequest`, `CancellationRequestResponse`, `SystemStatusResponse` y `ApiError`, sin exponer DNI completos, detalles internos ni ejemplos sensibles.
- Añadir pruebas que comparen las rutas expuestas con el documento generado y verifiquen operaciones, etiquetas, esquemas, restricciones, códigos de respuesta y disponibilidad de Swagger UI en desarrollo.
- Sincronizar el snapshot OpenAPI y los tipos TypeScript existentes cuando la documentación generada cambie.
- Incorporar en la documentación de desarrollo y en las pruebas de contrato la regla de que todo endpoint nuevo o modificado debe actualizar OpenAPI como parte de su Definition of Done.
- No añadir esquemas de autenticación, autorización, JWT, cookies de sesión ni contratos funcionales nuevos.

## Capabilities

### New Capabilities

Ninguna. El cambio amplía las capacidades existentes de backend e integración contractual.

### Modified Capabilities

- `backend-foundation`: permitir Swagger UI como dependencia y herramienta exclusivamente local de documentación, manteniendo el conjunto de dependencias mínimo y la exposición deshabilitada por defecto.
- `frontend-backend-integration`: ampliar el contrato OpenAPI existente para cubrir documentación navegable, todos los endpoints expuestos, DTO y errores, además de establecer una verificación obligatoria contra deriva documental.

## Impact

- Dependencias Maven de `/backend`: reemplazo del starter `webmvc-api` por `webmvc-ui`, conservando springdoc 3.0.3.
- Configuración común y local de springdoc y Swagger UI.
- Configuración OpenAPI, controladores, DTO y representación común de errores del backend.
- Pruebas web y de integración del backend; snapshot OpenAPI y tipos generados del frontend si cambia el contrato emitido.
- README del backend con rutas, uso de Swagger UI y regla de mantenimiento.
- No cambia el comportamiento funcional, la persistencia, MySQL, los contratos externos ni la seguridad del sistema.
