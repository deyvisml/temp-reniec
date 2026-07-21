## Why

La recuperación automática de una solicitud anterior ya no representa correctamente el flujo: un ciudadano que vuelve a ingresar puede querer cancelar otros certificados vigentes y no regresar al paso o constancia de un trámite previo. Cada entrada desde la página de inicio debe comenzar una consulta y una solicitud nuevas, conservando el historial únicamente para trazabilidad.

## What Changes

- Eliminar la recuperación automática de solicitudes y del progreso mediante DNI.
- Crear una solicitud nueva y ejecutar una consulta actualizada de certificados cada vez que se inicia el flujo desde la página de inicio.
- Conservar solicitudes finalizadas, certificados, selecciones, revocaciones y constancias anteriores como historial, sin convertirlos en el contexto activo del nuevo ingreso.
- Marcar como `ABANDONED` una solicitud anterior no confirmada cuando un nuevo inicio válido la sustituya, sin crear tablas de sesiones, ventanas de recuperación ni columnas nuevas.
- Impedir de forma controlada un nuevo inicio cuando exista una revocación confirmada todavía en curso o con resultado incierto; esa protección no recuperará ni mostrará el trámite anterior.
- Evitar que volver a ingresar después de completar una cancelación lleve al ciudadano nuevamente a la constancia anterior; una eventual consulta histórica de constancias será una funcionalidad separada y autenticada.
- Mantener la continuación dentro del trámite actual como navegación normal, diferenciándola de una recuperación posterior desde la página de inicio.
- **BREAKING**: cambiar `POST /api/v1/cancellation-requests` para que solo inicie solicitudes y eliminar el campo `reused` de la respuesta, OpenAPI y tipos TypeScript.
- Actualizar el contexto, las decisiones técnicas, el modelo documentado y las pruebas que actualmente establecen recuperación indefinida.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `cancellation-request-persistence-model`: reemplazar la recuperación indefinida por historial preservado, sustitución controlada de solicitudes previas no confirmadas y bloqueo de operaciones ya confirmadas que continúan en curso o inciertas.
- `citizen-eligibility-entry`: hacer que cada envío nuevo desde inicio cree una solicitud y consulta nuevas, eliminar la semántica `reused` y mantener control transaccional frente a envíos concurrentes.

## Impact

- Backend: coordinador de inicio/elegibilidad, repositorio de solicitudes, DTO de respuesta, descripciones Swagger/OpenAPI y pruebas de persistencia e integración.
- Frontend: contrato OpenAPI almacenado, tipos TypeScript generados y fixtures de pruebas; la vista no incorporará recuperación ni almacenamiento local del progreso.
- Persistencia: no requiere una migración estructural ni tablas nuevas; reutiliza `request_status`, `ABANDONED`, las relaciones históricas y los índices existentes por DNI/estado/fecha.
- Documentación: `PROJECT_CONTEXT.md`, decisiones técnicas y documentación del modelo deberán dejar de prometer recuperación o reanudación automática.
- Integraciones: cada nuevo inicio realizará una consulta fresca; no cambia todavía el contrato real del proveedor ni implementa ID Perú, revocación o descarga histórica de constancias.
