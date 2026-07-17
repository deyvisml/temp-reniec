## Why

El modelo actual separa correctamente varios conceptos del dominio, pero combina esa separación con UUID binarios, representaciones criptográficas todavía no implementadas, columnas generadas y restricciones excesivas. Esto dificulta inspeccionar, explicar y mantener la base sin aportar valor proporcional en la etapa actual.

La corrección debe evaluar cada tabla por su responsabilidad real y simplificar el esquema sin reducir arbitrariamente todo el trámite a una sola tabla.

## What Changes

- **BREAKING** Mantener las siete tablas actuales porque cada una representa una responsabilidad real, pero sustituir sus identificadores `BINARY(16)` por claves numéricas `BIGINT UNSIGNED AUTO_INCREMENT` y claves foráneas del mismo tipo.
- **BREAKING** Reemplazar `dni_lookup_hash`, `dni_ciphertext`, `dni_key_version` y `dni_last_four` por una sola columna legible `dni CHAR(8)` en `certificate_cancellation_request`.
- **BREAKING** Reemplazar `other_reason_ciphertext` y su versión de clave por `other_reason VARCHAR(300)`.
- Eliminar columnas generadas de guarda y aplicar las reglas de solicitud activa y operación vigente en casos de uso transaccionales cuando esas funcionalidades se implementen.
- Simplificar cada tabla eliminando campos especulativos o redundantes, sin eliminar la separación entre solicitud, consultas de elegibilidad, verificaciones de identidad, sesiones, revocaciones, constancias y auditoría.
- Conservar claves foráneas, unicidades indispensables, concurrencia optimista y los índices que respondan a consultas concretas.
- Mantener una referencia de sesión opaca y no secreta; no persistir JWT, refresh tokens ni credenciales en texto plano.
- Mantener una clave de idempotencia legible y única para cada operación de revocación, sin una columna generada adicional.
- Mantener una auditoría mínima y comprensible porque la cancelación de certificados requiere trazabilidad, sin convertirla en event sourcing ni guardar detalles técnicos extensos.
- Reemplazar la migración inicial y alinear entidades JPA, repositorios, pruebas y documentación si se confirma que no hay datos relevantes que conservar; de lo contrario, diseñar una migración hacia adelante.
- Mantener fuera de MySQL datos biométricos, contraseñas, credenciales, tokens, payloads externos completos y archivos PDF. El DNI legible tampoco se registrará en logs, errores ni URLs.

## Capabilities

### New Capabilities

No se introducen capacidades nuevas.

### Modified Capabilities

- `cancellation-request-persistence-model`: conserva el modelo relacional de siete responsabilidades y simplifica sus campos, identificadores, restricciones e índices.
- `backend-foundation`: alinea el alcance técnico con el modelo relacional simplificado y evita tanto la sobreingeniería como la eliminación indiscriminada de conceptos del dominio.

## Impact

Se modificarán la migración Flyway V1, las siete entidades JPA y sus repositorios, algunos enums, las pruebas de persistencia y `docs/data-model/README.md`. MySQL, Flyway, JPA, Testcontainers, Docker Compose y la integración técnica frontend-backend se mantienen. No se crearán endpoints funcionales, JWT, integraciones reales ni vistas ciudadanas.
