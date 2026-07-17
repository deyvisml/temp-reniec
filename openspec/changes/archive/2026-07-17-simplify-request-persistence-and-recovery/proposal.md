## Why

El modelo actual conserva campos y una tabla de sesiones que no responden a la necesidad real de recuperación del proyecto y dificultan explicar, consultar y mantener la base de datos. El progreso ya puede recuperarse localizando la solicitud vigente por DNI y leyendo su estado actual, por lo que conviene retirar infraestructura anticipada antes de continuar con JWT y las siguientes pantallas.

## What Changes

- **BREAKING** Eliminar `public_reference`, `consent_version`, `recoverable_until`, `expires_at` y las columnas de control optimista llamadas `version` del esquema y de las entidades JPA.
- **BREAKING** Eliminar completamente `cancellation_request_session`, su entidad, repositorio, relaciones, consultas y pruebas; la recuperación no creará ni administrará registros de sesión.
- Mantener `certificate_cancellation_request.id` como identificador simple de la solicitud y devolverlo como `requestId` en el contrato de inicio, sin incluir el DNI en URLs.
- Recuperar siempre el progreso de una solicitud ciudadana sin finalizar consultándola por DNI y utilizando `request_status` como fuente directa del paso alcanzado.
- No aplicar expiración temporal a las solicitudes del MVP: una solicitud sin finalizar podrá retomarse aunque haya transcurrido tiempo, sin `recoverable_until`, `expires_at` ni transición automática a `EXPIRED`.
- Mantener la confirmación mediante `confirmed_at`; no persistir una versión de consentimiento mientras no exista un requisito institucional confirmado.
- Sustituir el control optimista basado en `version` por operaciones transaccionales y bloqueos explícitos solamente donde ya existe una necesidad concurrente demostrada.
- Aplicar una migración Flyway hacia adelante que preserve solicitudes y registros funcionales existentes mientras elimina las columnas y la tabla obsoletas.
- Actualizar OpenAPI, tipos TypeScript, navegación, pruebas, diagrama entidad-relación y documentación para reflejar un modelo de seis tablas.
- Aclarar en las decisiones técnicas que JWT podrá existir sin una tabla propia y que recuperar progreso significa retomar la solicitud vigente después de verificar nuevamente el DNI o la identidad según el punto del flujo.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `cancellation-request-persistence-model`: reducir el modelo a seis tablas, retirar los campos cuestionados y definir la recuperación directamente desde la solicitud vigente.
- `citizen-eligibility-entry`: reemplazar la referencia pública UUID por el identificador numérico de solicitud en el contrato y en la transición preparada al siguiente paso.
- `backend-foundation`: actualizar el límite de persistencia permitido para que describa el modelo simplificado de seis tablas y no una infraestructura de sesiones anticipada.

## Impact

- Migraciones Flyway y esquema MySQL.
- Entidades y repositorios JPA de solicitudes, sesiones y revocaciones.
- Caso de uso de inicio y recuperación de solicitudes.
- DTO y contrato OpenAPI de `POST /api/v1/cancellation-requests`.
- Tipos TypeScript, cliente HTTP y construcción de la transición del frontend.
- Pruebas unitarias, de persistencia, concurrencia, contrato e integración.
- `docs/data-model/README.md`, documentación de ejecución y decisiones técnicas.
- Este cambio depende funcionalmente de la implementación terminada `implement-home-dni-eligibility`; esa modificación debe incorporarse a las especificaciones principales antes de archivar esta corrección para que el delta de `citizen-eligibility-entry` tenga una base vigente.
