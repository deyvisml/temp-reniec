## Why

La identidad ya puede verificarse y la sesión JWT protege el flujo interno, pero el paso 2 todavía es solo una transición sin datos. El ciudadano necesita consultar después de autenticarse la lista vigente, elegir de forma segura uno o varios certificados y dejar esa selección persistida antes de avanzar al motivo.

## What Changes

- Incorporar el caso de uso autenticado que obtiene del segundo servicio los certificados vigentes de la solicitud actual y normaliza lista vacía, indisponibilidad, timeout, respuesta inválida y duplicados.
- Introducir un puerto propio para el segundo servicio, un adaptador mock determinista completo y la configuración necesaria para conectar un adaptador real únicamente cuando exista un contrato institucional verificado; no se inventarán URL, payloads ni credenciales.
- Persistir número de orden, fecha de creación, UUID canónico, disponibilidad y fecha de consulta en la entidad existente `cancellation_request_certificate`, sin crear una tabla adicional de selección.
- Exponer endpoints protegidos y versionados para obtener la lista y guardar una selección completa, validando sesión, identidad, pertenencia, disponibilidad, UUID, concurrencia y al menos un elemento.
- Sustituir la transición provisional posterior a ID Perú por la vista responsive y accesible de `docs/ui-reference/step-2.png`, con selección individual o total, contador, estados de carga y errores controlados.
- Actualizar estados de solicitud y sesión para autorizar el paso 3 solamente después de una selección válida, manteniendo bloqueado cualquier paso futuro todavía no implementado.
- Sincronizar OpenAPI, tipos TypeScript, documentación y pruebas de backend, frontend, persistencia e integración.
- Mantener fuera de alcance motivo, confirmación, revocación, constancia y cualquier contrato externo no confirmado.

## Capabilities

### New Capabilities

- `authenticated-certificate-listing`: Consulta post-autenticación, normalización, persistencia y exposición segura de los certificados vigentes de la solicitud activa.
- `citizen-certificate-selection`: Presentación accesible del paso 2 y persistencia autoritativa de una selección válida de uno o varios certificados.

### Modified Capabilities

- `cancellation-request-persistence-model`: La estructura reservada para certificados y selección pasa a ser utilizada por los casos de uso reales, con transiciones e integridad transaccional.
- `frontend-backend-integration`: Se agregan contratos protegidos de listado y selección, errores documentados, tipos generados y pruebas de sincronización.
- `id-peru-citizen-authentication`: El resultado verificado deja de mostrar la transición provisional y habilita la consulta real del paso 2.
- `id-peru-runtime-mode`: La ruta interna configurada conserva el paso 2 después del retorno exitoso de ID Perú en cada ambiente.
- `project-reference-materials`: La documentación registra el segundo servicio implementado, sus límites y los escenarios disponibles para desarrollo.

## Impact

- Backend: módulos de sesión, identidad, certificados, persistencia, errores, configuración y OpenAPI.
- Frontend: flujo interno, vista del paso 2, cliente HTTP, tipos generados, estados y pruebas responsive/accesibles.
- Base de datos: reutilización de `cancellation_request_certificate`, estados y auditoría existentes; solo se añadirá una migración incremental si la implementación demuestra una restricción o índice faltante.
- Integraciones: nuevo límite de proveedor posterior a ID Perú. El mock permitirá completar el flujo local y automatizado; la conexión real quedará condicionada a recibir y validar el contrato oficial.
- Prerrequisito: la sesión JWT y la integración ID Perú de SPEC-13 deben estar completas y vigentes antes de aplicar este cambio.
