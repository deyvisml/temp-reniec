## Why

El proyecto ya dispone de bases técnicas, persistencia e integración entre aplicaciones, pero todavía no ofrece una funcionalidad ciudadana. Se necesita implementar el inicio real del flujo para validar el DNI, crear o recuperar una solicitud compatible y consultar de forma controlada si existen certificados digitales susceptibles de cancelación.

## What Changes

- Reemplazar la página técnica temporal por una página de inicio responsive y accesible basada en `docs/ui-reference/home.png`, conservando el contexto funcional como autoridad ante cualquier diferencia visual.
- Incorporar un formulario de DNI con una regla centralizada de exactamente ocho dígitos numéricos, validada tanto en frontend como en backend, sin colocar el DNI en URLs ni almacenamiento web.
- Exponer un endpoint REST versionado que cree o reutilice de forma transaccional una solicitud activa compatible, registre cada consulta de elegibilidad y devuelva un resultado normalizado sin revelar certificados individuales.
- Implementar un puerto de consulta de certificados y un adaptador mock determinista y reemplazable que cubra elegibilidad, no elegibilidad, indisponibilidad, resultado no concluyente y error técnico con DNI ficticios documentados.
- Actualizar los estados de la solicitud y del intento de elegibilidad de forma consistente, con control de duplicados, concurrencia optimista, expiración y reintentos seguros sobre las tablas existentes.
- Interpretar en el frontend todos los resultados funcionales y técnicos, evitar envíos duplicados, presentar carga y mensajes accesibles, permitir reintentos seguros y habilitar la transición futura a verificación de identidad solo cuando el backend lo autorice.
- Actualizar OpenAPI y los tipos TypeScript generados, además de añadir pruebas unitarias, de integración con MySQL y de contrato que demuestren el flujo extremo a extremo.
- Mantener fuera de alcance JWT, ID Perú, motivos, confirmación, revocación, constancia y cualquier módulo administrativo.

## Capabilities

### New Capabilities

- `citizen-eligibility-entry`: Inicio ciudadano de una solicitud, validación del DNI, consulta normalizada de elegibilidad, mock externo reemplazable, resultados de continuidad y experiencia completa de la página de inicio.

### Modified Capabilities

- `backend-foundation`: La base técnica deja de ser exclusivamente no funcional y admite el primer endpoint ciudadano conservando errores, correlación, validación y arquitectura simple.
- `frontend-foundation`: La página temporal se sustituye por la página ciudadana real y su formulario accesible, manteniendo el App Router y los estados globales existentes.
- `frontend-backend-integration`: La comprobación visual temporal se retira de la portada y la integración reutilizable pasa a soportar el contrato funcional versionado y sus tipos sincronizados.
- `cancellation-request-persistence-model`: Se activan las reglas transaccionales para solicitud activa, intentos de elegibilidad, transiciones y concurrencia sobre el esquema consolidado, sin tablas nuevas.

## Impact

- **Backend:** nuevo módulo funcional dentro del monolito modular para API, caso de uso, contrato de integración y mock; ampliación del manejo de errores y de las consultas de repositorio existentes.
- **Persistencia:** uso de `certificate_cancellation_request` y `certificate_eligibility_check`; no se prevén tablas nuevas y cualquier ajuste de índice o restricción deberá gestionarse con Flyway.
- **Frontend:** reemplazo de la portada temporal, actualización del layout institucional, cliente funcional de elegibilidad y estados de interacción; retiro del indicador técnico visible.
- **Contratos:** OpenAPI del backend como fuente de verdad y regeneración comprobable de tipos TypeScript.
- **Pruebas:** suites rápidas aisladas y pruebas reales con MySQL/Testcontainers para transacciones, duplicados, concurrencia y comunicación frontend-backend.
- **Dependencias:** no se incorporarán librerías adicionales salvo una necesidad demostrada durante la implementación.
