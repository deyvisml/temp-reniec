## Why

El contexto vigente todavía afirma que la pantalla de inicio obtiene y conserva la lista detallada de certificados, aunque la regla confirmada separa dos integraciones: una consulta inicial de existencia y, después de autenticar al ciudadano, un listado detallado. Mantener ambas responsabilidades mezcladas expone datos antes de tiempo, confunde los estados y deja el modelo de certificados vinculado al intento equivocado.

## What Changes

- Corregir `PROJECT_CONTEXT.md`, referencias UI, decisiones y documentación vigente para distinguir el servicio inicial booleano de existencia del futuro servicio de listado del paso 2.
- Mantener la pantalla de inicio limitada a validar el DNI, crear una solicitud nueva y consultar si existen certificados disponibles, sin recibir, mostrar ni persistir cantidad, número de orden, fecha de creación o UUID.
- Renombrar el contrato interno de “elegibilidad” a “disponibilidad/existencia” y normalizar por separado resultado positivo, negativo, inconcluso, indisponibilidad, timeout y error técnico; ningún fallo se interpretará como ausencia confirmada.
- **BREAKING**: actualizar el DTO/OpenAPI y los tipos TypeScript para sustituir la semántica ambigua `eligibilityResult=ELIGIBLE|NOT_ELIGIBLE` por un resultado explícito de disponibilidad, manteniendo estable la ruta versionada de inicio.
- Actualizar el puerto del primer servicio y su mock determinista para que representen exclusivamente una respuesta externa booleana y nunca construyan objetos de certificados.
- Diferenciar estados de existencia confirmada, ausencia confirmada, consulta fallida, autenticación pendiente, autenticación completada con listado pendiente y lista detallada disponible.
- Aplicar una migración Flyway incremental que renombre la persistencia de la consulta inicial a disponibilidad, preserve los intentos existentes y desacople `cancellation_request_certificate` del intento inicial. La tabla de certificados se conserva vacía y disponible para el futuro segundo servicio.
- Actualizar casos de uso, manejo de errores, documentación OpenAPI, snapshot/tipos generados y pruebas de backend, frontend, MySQL y contrato.
- Documentar el escenario posterior en que la consulta inicial fue positiva pero el segundo servicio devuelve una lista vacía, sin implementarlo todavía.

## Capabilities

### New Capabilities

Ninguna. La separación se incorpora en las capacidades existentes y no implementa anticipadamente el segundo servicio.

### Modified Capabilities

- `project-reference-materials`: corrige la fuente funcional y el índice visual para separar existencia inicial y listado posterior.
- `citizen-eligibility-entry`: redefine la operación inicial como consulta de disponibilidad sin certificados individuales y actualiza resultados, mock, estados, mensajes y pruebas.
- `cancellation-request-persistence-model`: renombra la persistencia de la consulta inicial, conserva los datos mediante migración incremental y desacopla los certificados del intento de existencia.
- `frontend-backend-integration`: actualiza OpenAPI, tipos TypeScript, cliente centralizado y verificación de ausencia de datos individuales en la respuesta inicial.
- `backend-foundation`: ajusta el límite técnico y la historia de migraciones al modelo corregido sin agregar el segundo servicio.

## Impact

- Backend: paquete/caso de uso de consulta inicial, puerto y mock, DTOs, estados, entidades y repositorios de la consulta, migración Flyway, OpenAPI y pruebas.
- Frontend: interpretación del resultado, tipos generados, cliente, mensajes ciudadanos y pruebas; no cambia el diseño general de la portada.
- Base de datos: migración hacia adelante con preservación de solicitudes e intentos; se conserva `cancellation_request_certificate`, pero deja de depender de la consulta inicial.
- Documentación: contexto funcional, referencias UI, modelo de datos, integración local y descripciones Swagger afectadas.
- No se agregan dependencias, autenticación, segundo servicio, selección, motivo, revocación ni constancia.
