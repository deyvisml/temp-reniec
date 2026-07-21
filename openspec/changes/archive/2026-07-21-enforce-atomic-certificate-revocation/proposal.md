## Why

El dominio vigente permite resultados parciales al cancelar varios certificados seleccionados, lo que complica la consistencia del trámite, la comunicación ciudadana y la constancia. La nueva decisión de negocio exige que cada operación trate el conjunto seleccionado de forma atómica: se cancelan todos los certificados elegidos o no se cancela ninguno.

## What Changes

- **BREAKING**: eliminar el resultado general `PARTIAL` y cualquier estado que represente una cancelación confirmada solo para parte del conjunto seleccionado.
- Mantener la selección flexible: el ciudadano puede elegir uno, varios o todos los certificados disponibles; “todos o ninguno” se aplica exclusivamente a los certificados seleccionados.
- Exigir que la integración de revocación procese la lista de UUID como una única operación atómica e idempotente y devuelva un resultado global confirmado.
- Mantener `OUTCOME_UNKNOWN` cuando no sea posible confirmar todavía si la operación atómica se ejecutó, sin asumir ni comunicar una cancelación parcial.
- Simplificar la persistencia eliminando `certificate_revocation_result` y usando `revocation_operation` como fuente del resultado técnico global; los certificados involucrados se obtienen de la selección inmutable de la solicitud confirmada.
- Actualizar estados del backend, agregación, repositorios, migraciones, pruebas y documentación del modelo para retirar estructuras dedicadas a resultados individuales.
- Actualizar `PROJECT_CONTEXT.md`, decisiones, referencias UI y especificaciones vigentes para sustituir resultados por certificado y constancias parciales por una constancia única del resultado atómico.
- Conservar las imágenes de referencia sin regenerarlas; cualquier texto visual incompatible quedará documentado como no vigente hasta implementar la vista correspondiente.

## Capabilities

### New Capabilities

- `atomic-certificate-revocation`: Define la semántica de selección flexible, revocación atómica e idempotente, tratamiento del resultado incierto y constancia única para el conjunto seleccionado.

### Modified Capabilities

- `cancellation-request-persistence-model`: Simplifica el esquema y el backend retirando resultados individuales y estados parciales, y mantiene el resultado global en la operación de revocación.
- `project-reference-materials`: Actualiza la fuente funcional, las decisiones y la interpretación de las referencias visuales conforme a la regla de todos o ninguno.

## Impact

- Documentación: `docs/context/PROJECT_CONTEXT.md`, `docs/TECHNICAL_DECISIONS.md`, `docs/data-model/README.md` y `docs/ui-reference/README.md`.
- Persistencia: nueva migración Flyway hacia adelante para retirar `certificate_revocation_result`; actualización del diagrama, consultas y pruebas de cobertura de comentarios.
- Backend: entidades, repositorios, enums, cálculo de resultado general y pruebas asociados a resultados individuales o parciales.
- Contrato futuro de revocación: deberá aceptar una lista de UUID y garantizar atomicidad para el conjunto; no se inventará todavía el contrato institucional de transporte.
- Frontend futuro: selección sin cambios, pero confirmación, resultado y constancia no ofrecerán ni mostrarán resultados parciales.
- No se incorporan todavía la integración real de revocación, las vistas restantes ni la generación real de la constancia.
