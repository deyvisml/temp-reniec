## Why

El flujo ciudadano ya permite autenticar al titular, consultar y seleccionar certificados y registrar un motivo, pero todavía no dispone de una revisión autoritativa ni de una confirmación expresa que cierre la etapa reversible. Este cambio incorpora el límite transaccional previo a la revocación, de modo que el ciudadano conozca exactamente qué confirmará y el backend inmovilice la solicitud sin ejecutar aún la operación externa.

## What Changes

- Incorporar el paso 4 de cinco en la ruta interna vigente, basado en `docs/ui-reference/step-4.png` pero corregido para el flujo actual y el conjunto de certificados expresamente seleccionado.
- Exponer desde el backend un resumen autoritativo con DNI parcialmente oculto, certificados seleccionados, motivo y consecuencias; el frontend no enviará ni reconstruirá esos datos como fuente de verdad.
- Exigir un consentimiento explícito antes de confirmar y registrar la fecha UTC y una versión estable, controlada por el backend, del texto presentado.
- Validar nuevamente sesión, identidad, selección, pertenencia y disponibilidad de certificados, motivo y estado de la solicitud dentro de una confirmación transaccional.
- Hacer idempotente el doble envío de la misma confirmación y rechazar solicitudes incompatibles, manipuladas, terminales o concurrentes.
- Inmovilizar motivo y selección después de confirmar y dejar la solicitud en `CONFIRMED`, preparada inequívocamente para una futura revocación, sin ejecutarla en este incremento.
- Permitir regresar al paso 3 antes de confirmar, mantener bloqueados los pasos futuros y resolver recargas o sesiones expiradas mediante el flujo interno existente.
- Actualizar OpenAPI, tipos TypeScript, documentación y pruebas de backend, frontend e integración.

## Capabilities

### New Capabilities

- `cancellation-review-confirmation`: Resumen autoritativo, consentimiento expreso, confirmación idempotente y comportamiento completo del paso 4 protegido.

### Modified Capabilities

- `cancellation-request-persistence-model`: La solicitud confirmada almacenará la versión del consentimiento efectivamente mostrado, además de la fecha de confirmación, y reforzará la inmutabilidad posterior.

## Impact

- Backend Spring Boot: nuevos DTO, controlador y servicio de revisión/confirmación, validaciones transaccionales, auditoría y manejo uniforme de errores.
- Persistencia MySQL/JPA: migración Flyway incremental y campo de versión de consentimiento en la solicitud existente; no se creará una tabla de confirmaciones ni se duplicará el resumen.
- Frontend Next.js: vista del paso 4, carga protegida, consentimiento accesible, navegación al paso 3 y estados de procesamiento/error dentro de la ruta canónica.
- Contratos: OpenAPI y tipos TypeScript sincronizados para consultar el resumen y confirmar.
- Pruebas: cobertura unitaria, de persistencia, HTTP, componentes y flujo integrado; no se incorporarán nuevas dependencias de producción.
