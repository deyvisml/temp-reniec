## Context

El paso 2 usa actualmente un `Set` de UUID en el frontend, casillas de verificación y una operación `PUT` que acepta hasta cien UUID. El backend bloquea la solicitud y todas sus filas antes de reemplazar el conjunto seleccionado, mientras el paso 4 consulta una lista de filas seleccionadas. La tabla `cancellation_request_certificate` ya contiene `selected`, `selected_at` y `version`, por lo que el cambio no necesita otra entidad ni una tabla de selección.

La nueva referencia visual conserva composición útil —tarjetas de certificados, jerarquía y acciones—, pero muestra dos selecciones y textos plurales que contradicen la regla nueva. El contexto funcional y esta especificación prevalecen sobre esos detalles de la imagen.

## Goals / Non-Goals

**Goals:**

- Garantizar una sola selección activa por solicitud en UI, API, transacción y MySQL.
- Permitir cambiar el certificado elegido antes de avanzar, reemplazando la selección anterior de forma atómica.
- Mantener la lista completa de certificados consultados sin rediseñar el modelo.
- Presentar y confirmar un solo certificado autoritativo en el paso 4.
- Corregir contratos, documentación y pruebas que todavía describen conjuntos.

**Non-Goals:**

- Implementar el servicio de revocación o la constancia.
- Cambiar el segundo servicio que devuelve la lista completa de certificados vigentes.
- Crear una tabla de selección, mover el certificado elegido a la solicitud o eliminar certificados no elegidos.
- Alterar las imágenes de referencia.

## Decisions

### 1. El contrato de escritura usará un UUID singular

`CertificateSelectionRequest` expondrá `certificateUuid` en vez de `certificateUuids`. Un array, un valor vacío o campos desconocidos no representarán una selección válida. Esto expresa la regla en el tipo y evita aceptar una lista para luego limitarla artificialmente a tamaño uno.

Se conservará la respuesta de listado con todas las filas y su indicador `selected`; cualquier contador restante estará limitado a `0` o `1`. El resumen del paso 4 cambiará de `certificates` a `certificate`, porque una colección permitiría que consumidores futuros vuelvan a asumir selección múltiple.

Alternativa descartada: conservar arrays con `@Size(max=1)`. Aunque sería un cambio menor, mantendría una semántica de conjunto innecesaria en frontend, OpenAPI y futuras integraciones.

### 2. La UI usará selección exclusiva nativa

Cada tarjeta usará un `input type="radio"` con el mismo nombre de grupo. Elegir otra tarjeta reemplazará inmediatamente la elección local; no existirá “seleccionar todos”. La acción Continuar solo se habilitará con un UUID elegido y sin envío activo.

La pantalla seguirá apareciendo con un único certificado y no lo seleccionará automáticamente. Los textos dirán “Selecciona un certificado digital vigente” y “1 certificado seleccionado”. La imagen adjunta se usará para disposición visual, no para conservar casillas, dos tarjetas activas ni redacción plural.

### 3. El reemplazo se serializará sobre la solicitud

El coordinador mantendrá el bloqueo pesimista de la solicitud y consultará las filas para actualización. Validará que el UUID singular sea canónico, pertenezca a la solicitud y continúe disponible. Dentro de la misma transacción seleccionará esa fila y deseleccionará cualquier otra. Repetir el mismo UUID conservará `selected_at` y será idempotente.

Alternativa descartada: guardar `selected_certificate_id` también en la solicitud. Duplicaría la fuente de verdad y exigiría sincronizarla con `selected` y `selected_at`.

### 4. MySQL impedirá una segunda selección activa

Una migración Flyway incremental creará un índice único funcional sobre la expresión que devuelve `request_id` solo cuando `selected = true` y `NULL` en los demás casos. MySQL permite varios valores nulos, pero solo una fila seleccionada por solicitud. No se agregará una columna JPA, tabla ni trigger.

El índice es defensa adicional frente a escrituras concurrentes o futuras rutas que omitan el coordinador. La migración no elegirá silenciosamente un certificado si encuentra datos anteriores con varias selecciones: fallará de forma segura. Como el proyecto aún está en etapa preproductiva, los entornos locales con datos ficticios incompatibles deberán reiniciarse; cualquier dato institucional real requeriría una decisión explícita antes del despliegue.

### 5. El paso 4 validará exactamente una fila

La consulta y la confirmación exigirán que exista exactamente un certificado seleccionado, disponible y perteneciente a la solicitud. El DTO de resumen presentará ese certificado como objeto singular. La confirmación seguirá bloqueando modificaciones posteriores y no llamará a revocación.

### 6. Las fuentes vigentes se corregirán sin reescribir archivos históricos

Se actualizarán `PROJECT_CONTEXT.md`, decisiones técnicas, modelo de datos, README visual y especificaciones principales. Los cambios OpenSpec archivados permanecerán como historial. La futura revocación se describirá para un UUID y un resultado por operación, sin implementar aún el proveedor.

## Risks / Trade-offs

- **Datos locales con selecciones múltiples impiden crear el índice** → detectar el caso mediante prueba de migración, documentar el reinicio local y no descartar evidencia automáticamente.
- **Dos pestañas cambian la elección simultáneamente** → serializar por solicitud, conservar versiones optimistas y traducir conflictos a la respuesta controlada existente.
- **Clientes antiguos envían `certificateUuids`** → tratarlo como contrato incompatible, actualizar OpenAPI y regenerar los tipos en el mismo incremento.
- **La imagen sugiere selección múltiple** → registrar expresamente la inconsistencia y probar textos, radio group y estados de selección singular.
- **Especificaciones activas anteriores todavía hablan de conjuntos** → actualizar las fuentes vigentes y archivar los cambios en orden cronológico para que esta corrección sea la última regla aplicable.

## Migration Plan

1. Verificar que la base a migrar no contenga solicitudes con más de una fila seleccionada.
2. Aplicar la migración incremental con el índice único condicionado.
3. Desplegar conjuntamente backend, OpenAPI y frontend porque el contrato de selección y revisión es incompatible con clientes anteriores.
4. Ejecutar migraciones desde una base vacía y desde la versión actual con datos compatibles.
5. Para revertir antes de datos productivos, retirar primero el cliente nuevo, restaurar los contratos anteriores y eliminar el índice mediante una migración hacia adelante; Flyway no se revertirá manualmente.

## Open Questions

- El contrato institucional del segundo servicio continúa pendiente, pero no afecta esta regla porque dicho servicio sigue devolviendo la lista completa.
- El formato final de revocación y constancia permanece fuera de alcance; ambos deberán recibir un solo certificado cuando se implementen.
