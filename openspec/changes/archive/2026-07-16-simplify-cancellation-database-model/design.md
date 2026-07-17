## Context

El esquema vigente contiene siete tablas con responsabilidades diferenciadas, pero también UUID `BINARY(16)`, varias representaciones del DNI, campos cifrados sin proveedor criptográfico, dos columnas generadas y numerosas restricciones defensivas. La separación conceptual no es por pantalla ni por estado: corresponde a elementos que pueden repetirse o tener un ciclo de vida independiente. La complejidad accidental está principalmente en cómo se representan y protegen los datos, no en la mera cantidad de tablas.

El proyecto sigue en etapa inicial y no tiene datos funcionales relevantes confirmados. Esto permite reemplazar la V1 para obtener un esquema limpio, siempre que durante la aplicación se verifique esa precondición. El cambio de integración frontend-backend pendiente debe archivarse antes de aplicar esta corrección.

## Goals / Non-Goals

**Goals:**

- Obtener una base sólida que pueda explicarse tabla por tabla y columna por columna.
- Conservar solo tablas con cardinalidad, trazabilidad o ciclo de vida independiente justificados.
- Permitir consultar directamente el DNI y el estado actual de una solicitud.
- Reducir campos especulativos, identificadores binarios, restricciones, índices y enums innecesarios.
- Mantener integridad relacional, UTC, Flyway, JPA, Testcontainers y concurrencia optimista donde aporta valor.
- Preservar la diferencia entre la solicitud ciudadana y la operación técnica de revocación.

**Non-Goals:**

- Forzar una única tabla o mantener siete tablas sin evaluarlas.
- Implementar endpoints ciudadanos, consulta real de certificados, ID Perú, JWT, revocación o generación de PDF.
- Inventar contratos externos, cifrado institucional, retención productiva o controles de infraestructura.
- Usar event sourcing, triggers complejos, procedimientos almacenados o tablas de catálogo para estados.

## Decisions

### Evaluación tabla por tabla

| Tabla | Decisión | Justificación |
| --- | --- | --- |
| `certificate_cancellation_request` | Conservar y simplificar | Es la raíz del trámite y la fuente directa de su estado actual. |
| `certificate_eligibility_check` | Conservar y simplificar | La consulta externa puede fallar o repetirse; cada intento tiene referencia, resultado y tiempo propios. |
| `identity_verification` | Conservar y simplificar | ID Perú admite cancelación, rechazo y nuevos intentos; no se guardará identidad hasheada ni biometría. |
| `cancellation_request_session` | Conservar y simplificar | La recuperación desde otro dispositivo está documentada y requiere varias sesiones con vigencia e invalidación independientes. |
| `revocation_operation` | Conservar y simplificar | Una operación técnica puede tener resultado incierto o reintento controlado y no debe confundirse con la solicitud ciudadana. |
| `cancellation_receipt` | Conservar y simplificar | La constancia tiene generación y disponibilidad propias; su falla no modifica una revocación exitosa. |
| `cancellation_audit_event` | Conservar en forma mínima | Un trámite sensible requiere trazabilidad cronológica, pero el evento no será fuente de verdad ni contendrá payloads extensos. |

La alternativa de una tabla única se descarta porque mezclaría el snapshot de la solicitud con colecciones de intentos, sesiones y operaciones, perdería historial real o produciría columnas repetidas. La alternativa de conservar el esquema sin cambios se descarta porque mantiene complejidad técnica sin infraestructura que la justifique.

### Identificadores numéricos y relaciones directas

Todas las tablas usarán `BIGINT UNSIGNED AUTO_INCREMENT`; las claves foráneas usarán `BIGINT UNSIGNED`. Esto facilita SQL, JPA, pruebas y explicación. Los identificadores internos no se expondrán directamente en URLs ciudadanas. Si una API futura necesita una referencia pública opaca, se agregará cuando exista esa necesidad.

Se conservarán las claves foráneas sin borrado en cascada. Las relaciones JPA seguirán siendo unidireccionales desde las tablas hijas y no habrá colecciones eager en la solicitud.

### Solicitud principal transparente

`certificate_cancellation_request` tendrá campos directos:

- `id`, `dni`, `request_status` y `eligibility_result`.
- `reason_code`, `other_reason`, `consent_version` y `confirmed_at`.
- `final_outcome`, `recoverable_until`, `expires_at`, `created_at`, `updated_at` y `version`.

Se elimina `lifecycle_status` porque duplica información derivable de `request_status`. También se eliminan `dni_lookup_hash`, `dni_ciphertext`, `dni_key_version`, `dni_last_four`, `other_reason_ciphertext`, `other_reason_key_version` y `active_dni_guard`.

El DNI se almacenará una vez como `CHAR(8)` y `other_reason` como `VARCHAR(300)`. Esta decisión busca transparencia para el alcance actual y no autoriza mostrar esos datos en logs, errores, URLs o endpoints técnicos.

### Tablas repetibles simplificadas

`certificate_eligibility_check` conservará: identificadores, número de intento, estado, resultado, referencia externa, fechas de solicitud/respuesta, código de error, correlación y creación.

`identity_verification` conservará: identificadores, número de intento, proveedor, estado, referencia externa, resultado de coincidencia con el DNI, fechas, código de error/cancelación, correlación y creación. Se elimina `verified_identity_hash` porque no existe un uso confirmado.

Las unicidades `(request_id, attempt_number)` se mantienen porque expresan una regla clara y fácil de explicar.

### Sesiones sin anticipar JWT

`cancellation_request_session` conservará: identificadores, `session_reference`, creación, expiración, último uso, invalidación, motivo y actualización. `session_reference` será una referencia opaca única que no autentica por sí sola.

Se eliminan `session_reference_hash`, `token_family_id` y `client_reference_hash`. La tarea futura de JWT definirá el almacenamiento de hashes de refresh token si realmente los necesita. Nunca se guardarán tokens o credenciales en texto plano.

### Revocación y constancia independientes

`revocation_operation` conservará una `idempotency_key VARCHAR(64)` única y legible, intento, estado, referencia externa, fechas principales, resultado, error, correlación y versión. Se eliminan `open_request_guard` y `next_status_check_at`; una futura reconciliación podrá agregar programación cuando exista contrato real.

`cancellation_receipt` conservará sus relaciones, código único, estado, referencia de almacenamiento, fechas y error. Se eliminan `document_hash` y `template_version` hasta que la generación real demuestre su necesidad. El PDF nunca se almacenará como BLOB.

### Auditoría mínima

`cancellation_audit_event` conservará identificadores, tipo de evento, estado anterior/nuevo, resultado, correlación, origen y fecha. Se eliminan `external_reference`, `technical_code` y `technical_detail`; los detalles operativos pertenecerán a la tabla técnica correspondiente o a logs sin datos personales.

La tabla será append-only desde la aplicación. No se reproducirán eventos para reconstruir el estado.

### Restricciones e índices proporcionales

La base conservará claves primarias, foráneas, unicidad de intentos, `session_reference`, `idempotency_key` y `receipt_code`, además de validaciones sencillas de DNI, intentos positivos y orden temporal. Los valores de estado serán enums controlados por el backend y columnas `VARCHAR`; no se duplicará todo el catálogo mediante grandes `CHECK` en SQL.

Se mantendrán índices solo para: historial/solicitud activa por DNI, expiración, último intento de elegibilidad e identidad, sesiones vigentes, operación actual, constancia disponible y auditoría cronológica.

Las columnas generadas para impedir solicitudes u operaciones simultáneas se eliminan. Esas reglas se aplicarán transaccionalmente cuando existan los casos de uso y se conozca con precisión qué estados son incompatibles.

## Risks / Trade-offs

- **El DNI será visible para quien tenga acceso SQL** → limitar accesos por ambiente y mantener el DNI fuera de logs, errores, URLs y endpoints técnicos; reevaluar si aparece un requisito institucional de cifrado.
- **La unicidad de solicitud activa no queda codificada con una columna generada** → implementarla y probarla en la transacción de creación cuando ese caso de uso exista.
- **Conservar siete tablas exige joins para historiales** → aceptar el costo porque representan relaciones uno-a-muchos reales; el estado actual continuará visible en la solicitud principal.
- **La tabla de sesiones precede a JWT** → limitarla a vigencia e invalidación; agregar campos de token solo junto con el diseño real de autenticación.
- **Reemplazar V1 rompe volúmenes locales existentes** → hacerlo solo con datos desechables; ante datos relevantes, detenerse y crear una migración hacia adelante.
- **Los identificadores secuenciales podrían enumerarse si se exponen** → no usarlos como referencias públicas en futuras APIs.

## Migration Plan

1. Archivar o sincronizar primero `establish-frontend-backend-integration` para no mezclar especificaciones pendientes.
2. Confirmar que las bases afectadas no contienen información relevante.
3. Reemplazar la V1 por el esquema simplificado de siete tablas y recrear solo volúmenes locales desechables.
4. Alinear las siete entidades, repositorios, enums y consultas con los tipos y campos nuevos.
5. Reescribir las pruebas de Flyway, relaciones, consultas, unicidades y concurrencia.
6. Actualizar el diagrama y documentar la justificación y columnas de cada tabla.
7. Ejecutar `mvn clean verify` y validar el arranque local con MySQL, Flyway y Hibernate.

La reversión antes de datos relevantes consiste en restaurar la migración y clases anteriores y recrear el volumen desechable. Con datos relevantes, toda modificación requerirá una migración explícita hacia adelante.

## Open Questions

No quedan decisiones abiertas para la propuesta. El cifrado institucional del DNI, el ciclo JWT, contratos externos, retención y detalles de generación de constancias se decidirán en las tareas funcionales correspondientes.
