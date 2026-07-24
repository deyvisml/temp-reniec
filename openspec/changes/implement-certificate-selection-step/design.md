## Context

La consulta pública solo confirma existencia. Después, ID Perú valida la identidad y SPEC-13 mantiene una sesión JWT HttpOnly vinculada con la solicitud activa. El esquema ya contiene `cancellation_request_certificate` con número de orden, fecha de emisión, UUID, disponibilidad, selección, timestamps y versión optimista, pero ningún caso de uso lo puebla todavía. El frontend muestra una transición provisional en lugar del paso 2.

El contrato institucional del segundo servicio no está incluido en el repositorio. Por eso el límite interno puede definirse y probarse ahora, pero un adaptador real no puede inventar endpoints, autenticación o payloads. La referencia visual es `docs/ui-reference/step-2.png`; el contexto funcional prevalece sobre cualquier texto contradictorio de la imagen.

## Goals / Non-Goals

**Goals:**

- Obtener la lista detallada solo para la solicitud de la sesión cuya identidad ya fue verificada.
- Persistir una instantánea autoritativa y reutilizarla durante la operación actual.
- Permitir una selección completa, transaccional y validada en el backend.
- Resolver lista vacía, uno o varios certificados, fallos técnicos, datos inválidos y concurrencia.
- Reemplazar la transición provisional por una experiencia accesible y responsive del paso 2.
- Dejar OpenAPI, tipos, documentación y pruebas sincronizados.

**Non-Goals:**

- Implementar motivo, confirmación, revocación, constancia o recuperación de operaciones anteriores.
- Inventar el contrato, seguridad, URL o credenciales del proveedor institucional.
- Crear otra tabla para selección, catálogo de estados o payloads externos completos.
- Confiar en DNI, requestId, certificados o estados enviados por el navegador.

## Decisions

### 1. El backend identifica la solicitud únicamente desde la sesión

Los endpoints serán recursos protegidos sin DNI ni requestId elegible por el cliente: `GET /api/v1/cancellation-requests/current/certificates` para consultar y `PUT /api/v1/cancellation-requests/current/certificate-selection` para reemplazar la selección actual. El access token aporta solo identificadores técnicos y el backend valida sesión persistida, solicitud y estado.

Se descarta recibir un requestId o DNI desde el frontend porque permitiría referencias cruzadas. Se usa `PUT` para que repetir exactamente la misma selección sea idempotente.

### 2. El segundo servicio se encapsula tras un puerto propio

El dominio consumirá un puerto que recibe el DNI obtenido de la solicitud y devuelve un resultado normalizado: lista válida, lista vacía, indisponibilidad, timeout o respuesta inválida. Un adaptador mock determinista cubrirá cero, uno, varios, duplicados, UUID inválido y fallos técnicos sin valores aleatorios.

El adaptador real solo se añadirá si durante la implementación existe un contrato institucional verificable. Hasta entonces no habrá una clase “real” que apunte a una URL ficticia. Esta separación permite incorporarlo después sin cambiar API, dominio, persistencia o UI.

### 3. La carga se reserva y completa en transacciones cortas

Una transacción bloquea la solicitud y cambia `AUTHENTICATED_PENDING_CERTIFICATE_LIST` a `CHECKING_CERTIFICATE_LIST`. La llamada externa ocurre fuera de la transacción. Otra transacción valida la respuesta completa y persiste todas las filas o ninguna. Una lista válida termina en `CERTIFICATES_AVAILABLE`; una lista vacía termina en `NO_CERTIFICATES_AVAILABLE`; un fallo técnico regresa a `AUTHENTICATED_PENDING_CERTIFICATE_LIST` para permitir reintento controlado.

Si una carga quedó reservada por una interrupción, un umbral configurable permite recuperarla de manera equivalente al patrón de consulta inicial. Las llamadas concurrentes no ejecutan dos persistencias; la segunda recibe el estado vigente o un conflicto controlado.

### 4. La instantánea persistida es la fuente de verdad del paso 2

La primera carga satisfactoria crea filas en `cancellation_request_certificate`. Las recargas posteriores de la misma operación leen esas filas y no vuelven a consultar al proveedor. La combinación `(request_id, certificate_uuid)` existente impide duplicados. Se valida UUID canónico, número de orden no vacío, fecha válida y ausencia de UUID u órdenes duplicados antes de guardar.

No se agrega una tabla de intentos de listado: para este MVP el estado de la solicitud, la auditoría y la correlación cubren el diagnóstico sin introducir otra entidad. Tampoco se modifica una migración ya aplicada; solo habrá V8 si una comprobación de implementación demuestra que falta una restricción o índice real.

### 5. La selección es un reemplazo autoritativo del conjunto

El frontend envía únicamente una lista no vacía de UUID canónicos. El backend carga todos los certificados de la solicitud, rechaza duplicados, desconocidos o no disponibles y actualiza en una transacción los flags `selected` y `selected_at`. La solicitud pasa a `CERTIFICATES_SELECTED` y la sesión autoriza el paso de motivo, aunque dicho paso todavía se representará como transición controlada.

La versión optimista de cada certificado detecta escrituras concurrentes. Repetir el mismo conjunto devuelve éxito; un conflicto real devuelve el error estándar y obliga a recargar. La selección puede cambiar antes de la confirmación futura, pero nunca después de `CONFIRMED`.

### 6. La UI mantiene estado efímero y usa el contrato generado

El paso 2 se renderiza en la ruta interna vigente (`/autorizacion` local o `/cancelacion` según ambiente), conserva el header de sesión y el stepper común, y obtiene la lista mediante el cliente central con cookies. Checkboxes nativos, selección total, contador visible y región `aria-live` cubren teclado y lectores de pantalla. En móvil la tabla se transforma en tarjetas o filas apiladas sin desplazamiento horizontal obligatorio.

La selección vive en estado React hasta pulsar Continuar; no se escribe en localStorage, sessionStorage, URL ni cookies legibles. Una lista vacía o error se presenta con una única acción válida y no se confunde con un fallo de identidad.

### 7. El contrato externo y el contrato ciudadano permanecen separados

Los DTO públicos exponen solo número de orden, fecha de creación, UUID, disponibilidad, selección y versión necesaria para presentación/control. No exponen DNI, referencias del proveedor ni payloads. OpenAPI documenta autenticación por cookies, 401/403/409/422/503/504 y correlación. Los tipos TypeScript se regeneran desde el documento validado.

## Risks / Trade-offs

- [El contrato real aún no existe] → Mantener el puerto y mock completos; implementar el adaptador real solo con documentación oficial y pruebas de contrato.
- [El proveedor cambia entre la consulta inicial y el listado] → Tratar una lista vacía como resultado funcional actual, bloquear continuidad y no degradarlo a error de identidad.
- [Una interrupción deja `CHECKING_CERTIFICATE_LIST`] → Recuperación por umbral y reserva transaccional, sin proceso distribuido adicional.
- [Dos pestañas cambian selección] → Versión optimista, respuesta 409 y recarga desde la fuente persistida.
- [UUID visibles después de autenticar] → Exponerlos solo dentro de sesión válida, nunca en URL, logs o almacenamiento persistente del navegador.
- [La tabla de escritorio no cabe en móvil] → Representación responsive semánticamente equivalente y pruebas en anchos pequeños.

## Migration Plan

1. Completar y validar SPEC-13, incluida la migración V7 y la sesión vigente.
2. Implementar el puerto, mock, estados y caso de uso de carga sin alterar migraciones aplicadas.
3. Añadir V8 solo si se identifica una necesidad de esquema concreta y probar actualización V7→V8 y base vacía.
4. Incorporar endpoints/OpenAPI, sincronizar tipos y después reemplazar la transición frontend.
5. Habilitar el mock por defecto en local/test y mantener producción sin arranque de un adaptador inexistente.
6. Cuando llegue el contrato oficial, añadir el adaptador real detrás del mismo puerto y habilitarlo por configuración externa.

El rollback de código deshabilita los endpoints y restaura la transición del paso 2; los certificados ya persistidos se conservan como historial. No se ejecutarán migraciones destructivas automáticas.

## Open Questions

- ¿Cuál es el contrato oficial del segundo servicio: URL, autenticación, campos, formatos de fecha, límites y códigos de error?
- ¿El proveedor garantiza unicidad de número de orden además de UUID, o esa validación debe mantenerse solo en aplicación?
- ¿Existe un SLA institucional que requiera timeouts diferentes de los valores locales iniciales?
