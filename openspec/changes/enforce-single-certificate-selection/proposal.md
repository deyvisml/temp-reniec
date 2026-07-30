## Why

La implementación actual permite persistir y confirmar varios certificados dentro de una misma solicitud, pero la nueva regla establece que cada operación ciudadana cancela exactamente un certificado vigente. El ajuste debe aplicarse de extremo a extremo para que la interfaz, el contrato, la persistencia y la revisión no mantengan comportamientos contradictorios.

## What Changes

- **BREAKING**: sustituir el contrato de selección basado en una lista de UUID por un único UUID de certificado y el resumen del paso 4 basado en una colección por un certificado singular.
- Cambiar el paso 2 a selección exclusiva, sin casilla de selección total ni contador o textos de selección múltiple.
- Mantener el paso 2 incluso cuando solo exista un certificado y exigir una elección expresa antes de continuar.
- Permitir reemplazar la elección mientras la solicitud siga siendo editable, dejando exactamente una fila seleccionada.
- Validar en backend la pertenencia, disponibilidad y unicidad del certificado, rechazando cargas vacías, múltiples, arbitrarias o incompatibles con el estado actual.
- Reforzar la garantía transaccional con una restricción de base de datos que impida más de una fila seleccionada por solicitud, sin crear otra tabla ni rediseñar la entidad principal.
- Ajustar el paso 4 para presentar y confirmar un solo certificado seleccionado.
- Actualizar el contexto vigente, decisiones técnicas, modelo de datos, referencia visual, OpenAPI, tipos TypeScript y pruebas afectadas.
- Dejar documentado que la futura revocación y constancia trabajarán con un certificado por operación, sin implementarlas en este cambio.

## Capabilities

### New Capabilities
- `single-certificate-selection`: Selección ciudadana exclusiva en el paso 2 y revisión singular del certificado en el paso 4.

### Modified Capabilities
- `cancellation-request-persistence-model`: La selección persistida pasa de un conjunto a exactamente una fila activa por solicitud editable.
- `atomic-certificate-revocation`: La operación futura deja de manejar un conjunto atómico y queda definida para un solo certificado.
- `project-reference-materials`: El contexto, las decisiones y las referencias visuales deben prevalecer sobre los textos de selección múltiple presentes en la imagen.

## Impact

- Frontend: paso 2, transición al motivo, resumen del paso 4, accesibilidad y pruebas de componentes.
- Backend: DTO y validación de selección, coordinador transaccional, confirmación, errores y pruebas.
- Persistencia: migración Flyway incremental e índice único condicionado para una selección activa por solicitud; se conservan las tablas y columnas actuales.
- Contratos: OpenAPI, snapshot y tipos TypeScript cambian de colección a certificado único.
- Documentación: contexto funcional, decisiones técnicas, modelo de datos y guía de referencias UI.
- No se agregan dependencias ni se implementan revocación o constancia.
