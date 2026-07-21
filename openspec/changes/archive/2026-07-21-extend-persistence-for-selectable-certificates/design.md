## Context

La V1 vigente crea seis tablas centradas en `certificate_cancellation_request`: intentos de elegibilidad, verificaciones de identidad, operaciones de revocación, constancias y auditoría. El modelo conserva correctamente la solicitud como raíz y separa los ciclos de vida repetibles, pero la consulta solo termina en un resultado normalizado global y la revocación solo registra un resultado global.

El contexto v2 confirma que la consulta devuelve cero, una o varias emisiones con número de orden, fecha de creación y UUID; que la lista debe mantenerse ligada a la solicitud; que el ciudadano selecciona al menos una después de autenticarse; y que la revocación produce un resultado individual por UUID. La base puede evolucionar añadiendo dos tablas sin sustituir V1 ni remodelar las responsabilidades existentes.

## Goals / Non-Goals

**Goals:**

- Persistir cada certificado consultado y su procedencia dentro de una solicitud.
- Persistir la selección directamente sobre el certificado, incluyendo concurrencia optimista.
- Persistir un resultado individual por certificado y operación con integridad entre solicitudes.
- Derivar de forma determinista el resultado general a partir de los resultados individuales.
- Ejecutar V1+V2 desde una base vacía y V2 sobre una base existente con V1 y datos.
- Mantener el modelo legible, relacional y sin tablas o capas anticipadas.

**Non-Goals:**

- Cambiar el endpoint de inicio o el contrato OpenAPI.
- Consumir el servicio real o mock de consulta.
- Implementar autenticación, pantalla de selección, caso de uso de revocación o generación de constancia.
- Inventar el contrato externo definitivo ni almacenar payloads completos.
- Cifrar o duplicar el UUID en columnas técnicas no confirmadas.
- Cambiar el tratamiento simplificado del DNI, la recuperación del progreso o las tablas actuales no afectadas.

## Decisions

### 1. Evolución incremental mediante V2

`V1__create_cancellation_request_model.sql` permanecerá inmutable. Una nueva `V2__add_request_certificates_and_revocation_results.sql` añadirá las tablas, restricciones e índices. En una base limpia Flyway ejecutará V1 y V2; en una base existente aplicará únicamente V2 y conservará solicitudes e historiales.

Alternativa descartada: consolidar otra V1 limpia. La base ya puede contener información útil y SPEC-09 exige compatibilidad incremental.

### 2. Certificado consultado como hijo de la solicitud y del intento

`cancellation_request_certificate` tendrá:

- `id BIGINT UNSIGNED` como clave interna.
- `request_id` y `eligibility_check_id` obligatorios.
- `order_number VARCHAR(64)` para conservar ceros iniciales y una semántica institucional aún pendiente.
- `emission_created_at TIMESTAMP(6)` e `consulted_at TIMESTAMP(6)` en UTC.
- `certificate_uuid CHAR(36)` con juego ASCII y comparación binaria.
- `availability_status VARCHAR(32)`.
- `selected BOOLEAN`, `selected_at TIMESTAMP(6)` nullable.
- `version BIGINT UNSIGNED`, `created_at` y `updated_at`.

La unicidad `(request_id, certificate_uuid)` impide duplicar una emisión dentro de la solicitud. Una clave foránea compuesta `(request_id, eligibility_check_id)` apuntará a una clave candidata `(request_id, id)` de `certificate_eligibility_check`, garantizando que la consulta de origen pertenezca a la misma solicitud.

No habrá colección bidireccional en la solicitud. Los certificados se cargarán mediante repositorio cuando un caso de uso los necesite.

### 3. UUID legible, canónico y con exposición restringida

El UUID se normalizará al formato canónico de 36 caracteres y se almacenará una sola vez en la fila del certificado. No se añadirán `_cipher`, hash, versión de clave ni una representación duplicada porque el proyecto acordó un esquema transparente y todavía no existe infraestructura institucional de cifrado.

Su protección consiste en acceso restringido a MySQL, ausencia en logs y URLs, no exposición antes de autenticar al ciudadano, y validación backend de que todo UUID usado pertenece a la solicitud. No es una credencial de autenticación y conocerlo no autoriza una revocación.

### 4. Selección en la fila del certificado

`selected` y `selected_at` representan la decisión actual. Un `CHECK` exigirá que ambos sean coherentes: no seleccionado implica fecha nula y seleccionado implica fecha presente. `@Version` sobre `version` detectará ediciones concurrentes del mismo certificado. Métodos de entidad acotados seleccionarán o deseleccionarán mientras el estado lo permita; las reglas completas del flujo se implementarán después.

Alternativa descartada: `certificate_selection`. No existe historial de selecciones confirmado ni relación muchos-a-muchos que justifique otra tabla.

### 5. Resultado individual separado de la operación global

`certificate_revocation_result` tendrá:

- `id`, `request_id`, `revocation_operation_id` y `request_certificate_id`.
- `submitted_uuid` como snapshot exacto enviado.
- `result_status`: `PENDING`, `SUCCEEDED`, `FAILED` u `OUTCOME_UNKNOWN`.
- `result_code`, `result_message` y `external_reference` normalizados y acotados.
- `processed_at`, `correlation_id`, `version`, `created_at` y `updated_at`.

La unicidad `(revocation_operation_id, request_certificate_id)` impide duplicados durante reintentos. Para rechazar cruces entre solicitudes sin triggers, la tabla llevará `request_id` y usará dos claves foráneas compuestas:

- `(request_id, revocation_operation_id)` → `revocation_operation(request_id, id)`.
- `(request_id, request_certificate_id, submitted_uuid)` → `cancellation_request_certificate(request_id, id, certificate_uuid)`.

Así, el certificado pertenece a la misma solicitud que la operación y el UUID registrado coincide con el certificado. La redundancia de `request_id` y `submitted_uuid` está justificada exclusivamente por integridad y trazabilidad del envío.

### 6. Resultado general derivado con precedencia conservadora

El resultado global no se asumirá uniforme. El repositorio permitirá contar resultados por estado y un calculador pequeño, sin I/O externo, aplicará:

1. Sin resultados o con algún `PENDING`: la operación no tiene resultado terminal.
2. Si existe algún `OUTCOME_UNKNOWN`: resultado general `OUTCOME_UNKNOWN`.
3. Todos `SUCCEEDED`: `SUCCEEDED`.
4. Todos `FAILED`: `FAILED`.
5. Mezcla de `SUCCEEDED` y `FAILED`: `PARTIAL`.

Cuando un futuro caso de uso finalice la operación, almacenará este valor derivado en `revocation_operation.normalized_result` y el correspondiente resultado final de la solicitud. La base preserva los detalles que permiten recalcularlo.

### 7. Estados nuevos sin romper datos anteriores

Los valores se almacenan como `VARCHAR`, por lo que no requieren tabla catálogo ni `ALTER ENUM`. Se añadirán valores backend para `NO_CERTIFICATES_AVAILABLE`, `CERTIFICATES_AVAILABLE`, `AUTHENTICATED_PENDING_SELECTION`, `CERTIFICATES_SELECTED`, `REVOCATION_SUCCEEDED`, `REVOCATION_PARTIAL`, `REVOCATION_FAILED` y `REVOCATION_OUTCOME_UNKNOWN`, además de `REVOCATION_PARTIAL` en el resultado final.

Los estados `ELIGIBLE`, `NOT_ELIGIBLE`, `COMPLETED`, `FAILED` y `OUTCOME_UNKNOWN` se conservarán temporalmente porque existen en el flujo implementado y podrían aparecer en datos V1. Esta tarea no reinterpreta ni migra automáticamente solicitudes históricas.

### 8. Borrado restringido y finalización conservadora

No se usará `ON DELETE CASCADE`. Una solicitud con certificados o resultados no podrá eliminarse físicamente por accidente. Finalizar una solicitud solo cambia su estado; conserva certificados, selección y resultados para constancia y trazabilidad.

## Risks / Trade-offs

- [Los valores de estado antiguos y nuevos coexistirán] → Documentar cuáles pertenecen al flujo vigente y conservar los antiguos únicamente para compatibilidad hasta que el caso de uso de consulta sea actualizado.
- [UUID legible en MySQL] → Aplicar acceso mínimo, no registrarlo ni exponerlo antes de autenticar, y reevaluar cifrado solo cuando exista una decisión institucional concreta.
- [Las claves foráneas compuestas añaden índices] → Limitar las claves candidatas a las dos invariantes críticas que evitan cruces de solicitud y UUID.
- [Un certificado podría cambiar de vigencia fuera del sistema] → `availability_status` permite reflejarlo posteriormente; esta tarea no implementa reconsulta ni sincronización externa.
- [El resultado global almacenado podría quedar desalineado] → Centralizar el cálculo, probar la matriz de estados y guardar el global únicamente al finalizar una operación.
- [Migración sobre datos reales inesperados] → V2 solo añade estructuras y claves candidatas sobre columnas existentes; probar expresamente el upgrade con filas V1 antes de aplicar.

## Migration Plan

1. Crear V2 con claves candidatas en tablas existentes y las dos tablas nuevas vacías.
2. Ejecutar Flyway desde cero en MySQL Testcontainers y validar las ocho tablas con Hibernate.
3. Ejecutar una prueba de upgrade que aplique V1, inserte datos representativos, aplique V2 y verifique su conservación.
4. Incorporar entidades, enums y repositorios unidireccionales alineados exactamente con V2.
5. Añadir pruebas de selección, unicidad, pertenencia, agregación, concurrencia y borrado restringido.
6. Actualizar `docs/data-model/README.md` y eliminar su aviso temporal de falta de alineación cuando el esquema ya coincida.

Rollback: el despliegue debe restaurar la base desde respaldo o aplicar una migración posterior explícita; Flyway no revertirá V2 automáticamente y no se eliminarán tablas que pudieran contener datos.

## Open Questions

- El significado exacto y longitud institucional del número de orden sigue pendiente; `VARCHAR(64)` evita asumir que es numérico.
- La zona horaria y semántica exacta de la fecha de creación deben confirmarse con el contrato externo; internamente se normalizará a UTC.
- La retención y eventual cifrado institucional del UUID permanecen pendientes de una política formal.
- Los códigos y mensajes definitivos del proveedor no están confirmados; solo se almacenarán valores normalizados y acotados.
