## Context

El contexto v2 y la persistencia extendida introdujeron selección de certificados y resultados técnicos independientes por certificado. El esquema efectivo tiene ocho tablas, incluida `certificate_revocation_result`, y el backend contiene estados `PARTIAL` y un agregador que calcula un resultado general a partir de filas individuales.

La nueva regla conserva la selección flexible, pero cambia la garantía de ejecución: la lista confirmada constituye una sola unidad atómica. Todos los certificados seleccionados quedan revocados o ninguno; una respuesta que no permita confirmar cuál de esos dos hechos ocurrió se considera incierta, nunca parcial. La integración real de revocación y la generación real de constancias todavía no existen.

Este cambio depende de que `update-context-and-ui-reference-flow`, `extend-persistence-for-selectable-certificates`, `start-new-request-without-progress-recovery` y `add-spanish-database-comments` sean la línea base sincronizada o archivada antes de aplicarlo.

## Goals / Non-Goals

**Goals:**

- Definir sin ambigüedad que la atomicidad cubre exactamente los certificados seleccionados.
- Eliminar del dominio, persistencia y documentación el resultado parcial confirmado.
- Simplificar el esquema retirando la tabla y el código destinados a resultados independientes.
- Mantener idempotencia y reconciliación segura cuando el resultado sea incierto.
- Alinear contexto, referencias UI, estados, constancia futura y pruebas con la nueva regla.

**Non-Goals:**

- Obligar al ciudadano a seleccionar todos los certificados disponibles.
- Implementar la pantalla de selección, ID Perú, revocación real o generación de PDF.
- Inventar el contrato institucional concreto del proveedor.
- Implementar compensaciones, rollback distribuido, sagas, colas o transacciones distribuidas.
- Modificar imágenes de referencia o crear módulos administrativos.

## Decisions

### 1. “Todos” significa todos los certificados seleccionados

El paso de selección continuará aceptando uno, varios o todos los certificados disponibles. Al confirmar, la selección queda inmutable y define el conjunto completo de UUID enviado a la operación. Los certificados no seleccionados permanecen fuera de la llamada y no cambian de estado.

Alternativa descartada: forzar la selección de todos los certificados vigentes. Cambiaría una decisión funcional distinta y eliminaría la libertad de cancelar solo el certificado que motiva el trámite.

### 2. Exigir atomicidad en el límite de integración

El futuro puerto de revocación enviará la lista completa bajo una única clave de idempotencia. Su resultado normalizado será global: éxito confirma que todos fueron revocados; fallo confirma que ninguno fue revocado; incertidumbre indica que todavía no puede afirmarse ninguno de los dos resultados.

No se intentará conseguir atomicidad llamando al proveedor una vez por UUID ni revirtiendo certificados ya revocados. Esa alternativa crea precisamente los estados parciales y compensaciones que la regla elimina. Si el proveedor institucional no garantiza atomicidad para una lista, la integración quedará bloqueada hasta acordar un contrato compatible.

### 3. Mantener solo tres resultados generales

`SUCCEEDED`, `FAILED` y `OUTCOME_UNKNOWN` serán los únicos resultados generales. Se retirarán `PARTIAL`, `REVOCATION_PARTIAL` y eventos equivalentes. `OUTCOME_UNKNOWN` no significa fallo: conserva la misma operación para consulta o reconciliación y bloquea una ejecución incompatible.

No se agregarán resultados individuales con el mismo valor común, porque duplicarían información sin aportar una decisión distinta.

### 4. Retirar `certificate_revocation_result`

Una migración Flyway V4 hacia adelante eliminará la tabla de resultados individuales y las claves o índices auxiliares que solo existen para relacionarla. V1, V2 y V3 permanecerán inmutables. El esquema efectivo tendrá siete tablas; `revocation_operation.normalized_result` será la fuente del resultado técnico y `certificate_cancellation_request.final_outcome` conservará el resultado ciudadano.

Los certificados incluidos se obtendrán de `cancellation_request_certificate` con `selected=true`. La aplicación impedirá modificar esa selección después de confirmar, utilizando la transacción y controles de estado existentes; no se añadirá otra tabla snapshot ni un trigger.

Se retirarán la entidad, repositorio, enum y agregador de resultados individuales. También se eliminarán restricciones candidatas redundantes creadas exclusivamente para sus claves foráneas, tras comprobar que ningún otro vínculo las usa.

Alternativa descartada: conservar la tabla con una fila idéntica por certificado. Mantendría 14 columnas, relaciones y concurrencia sin soportar una variación permitida por el negocio.

### 5. Constancia con lista y resultado común

La futura constancia enumerará o identificará los certificados seleccionados y mostrará un único resultado de operación. Solo podrá comunicar cancelación exitosa cuando el resultado global sea `SUCCEEDED`; con `FAILED` indicará que ninguno fue cancelado; con `OUTCOME_UNKNOWN` no afirmará éxito ni fallo y quedará pendiente de reconciliación.

La falla documental seguirá siendo independiente de una revocación ya confirmada.

### 6. Corregir fuentes vigentes sin reescribir historia

Se actualizarán `PROJECT_CONTEXT.md`, decisiones técnicas, modelo de datos, índice visual y especificaciones actuales. Las imágenes se conservarán sin regenerar; cualquier texto interno sobre resultados individuales o parciales se documentará como inconsistencia visual. Los cambios archivados permanecerán como historia y no se editarán.

## Risks / Trade-offs

- [El proveedor real podría no ofrecer atomicidad por lista] → Convertir la garantía en condición de aceptación del contrato; no simularla con llamadas individuales.
- [Un timeout puede ocultar una ejecución exitosa] → Mantener `OUTCOME_UNKNOWN`, la misma clave de idempotencia y una futura consulta de reconciliación antes de permitir otra operación.
- [Eliminar la tabla podría descartar datos de desarrollo] → Confirmar que no existe información relevante; si aparece un ambiente con datos que deban conservarse, detener el cambio y diseñar una migración de preservación antes de V4.
- [La selección mutable alteraría la evidencia de una operación] → Bloquear cambios de selección después de la confirmación y probar transiciones concurrentes.
- [Las imágenes todavía muestran resultados por certificado] → Conservarlas como composición visual y registrar explícitamente que el contexto atómico prevalece.

## Migration Plan

1. Sincronizar o archivar primero los cuatro cambios completos que forman la línea base V1–V3 y el contexto v2.
2. Actualizar el contexto y las especificaciones vigentes para fijar la regla atómica antes de modificar código.
3. Incorporar V4, retirando `certificate_revocation_result` y solo las claves auxiliares que queden sin uso.
4. Eliminar mappings y estados parciales, ajustar la selección inmutable y validar Hibernate.
5. Actualizar pruebas desde base vacía y desde V3, verificando preservación de solicitudes, certificados, selecciones, operaciones y constancias.
6. Actualizar documentación, comentarios/cobertura del esquema de siete tablas y referencias UI.
7. Validar que no quede `PARTIAL`, resultado individual o afirmación contradictoria en fuentes vigentes.

La reversión no editará V4. Si fuera necesaria antes de usar la funcionalidad real, se hará mediante otra migración hacia adelante que reconstruya la estructura; una vez adoptado el contrato atómico no se restaurará semántica parcial sin una nueva decisión de negocio.

## Open Questions

- El mecanismo concreto de reconciliación de `OUTCOME_UNKNOWN` dependerá del contrato institucional de revocación.
- El contenido y formato legal definitivo de la constancia continúa pendiente, pero deberá respetar el resultado común del conjunto seleccionado.
