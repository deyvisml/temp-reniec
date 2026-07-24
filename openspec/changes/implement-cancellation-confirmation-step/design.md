## Context

La solicitud ya concentra DNI, motivo, fecha de confirmación y estado; los certificados consultados y seleccionados están vinculados a esa misma solicitud. La sesión JWT identifica técnicamente la solicitud activa y la ruta interna resuelve los pasos sin exponer identificadores. El modelo incluso contiene `CONFIRMED` y protecciones parciales de inmutabilidad, pero no existe todavía un caso de uso HTTP que construya el resumen, obtenga el consentimiento y confirme de manera consistente.

La imagen `step-4.png` sirve como referencia de composición. Sus cuatro pasos y el texto que afirma que se cancelarán todos los certificados del DNI son obsoletos: la vista implementará el paso 4 de 5 y resumirá solo los certificados seleccionados. La revocación continuará siendo una operación posterior, atómica para ese conjunto.

## Goals / Non-Goals

**Goals:**

- Mostrar un resumen construido exclusivamente desde datos persistidos de la solicitud autenticada.
- Obtener un consentimiento expreso sobre un texto estable y versionado.
- Confirmar una sola vez de forma transaccional, idempotente y segura ante concurrencia.
- Inmovilizar la selección y el motivo y dejar la solicitud en `CONFIRMED`.
- Integrar el paso 4 en la ruta, sesión, stepper, contratos y estilos existentes.

**Non-Goals:**

- Consumir el servicio de revocación, crear una operación de revocación o avanzar a `REVOCATION_IN_PROGRESS`.
- Generar, descargar o simular una constancia.
- Permitir editar los datos desde el resumen o aceptar DNI, motivo o certificados enviados por el frontend.
- Crear una tabla de confirmaciones, duplicar el resumen o incorporar un motor configurable de consentimientos.
- Cambiar la regla atómica de revocación ni anticipar el contrato del proveedor externo.

## Decisions

### Dos operaciones sobre el recurso actual

El backend expondrá una lectura del resumen actual y una confirmación bajo `/api/v1/cancellation-requests/current`. Ambas resolverán la solicitud desde la sesión; no aceptarán `requestId`, DNI ni UUID en URL o cuerpo. Se prefieren operaciones explícitas frente a ampliar los DTO de selección o sesión, porque el resumen y la confirmación tienen validaciones y respuestas propias.

### Resumen autoritativo y minimizado

El servicio consultará la solicitud, el motivo y solo sus certificados seleccionados. La respuesta incluirá el DNI enmascarado, número de orden, fecha de creación, UUID abreviado para presentación, motivo legible, descripción de `OTHER`, texto y versión de consentimiento. No devolverá el DNI ni UUID completos. El frontend se limitará a representar esa respuesta.

Se descarta construir el resumen a partir del estado React o reenviar los certificados vistos en pasos anteriores, porque una recarga, manipulación o pestaña desactualizada podría confirmar un conjunto diferente al persistido.

### Consentimiento controlado por el backend

Un componente sencillo del backend definirá el texto ciudadano vigente y una versión estable (`CANCELACION_CERTIFICADOS_V1`). No será una variable de ambiente: cambiar el texto constituye un cambio funcional revisable. La lectura devolverá ambos valores y la confirmación recibirá solamente `consentAccepted: true` y la versión mostrada. El backend rechazará una versión obsoleta para obligar a recargar y leer el texto vigente.

Se descarta confiar solo en un checkbox del navegador o persistir el texto completo. El checkbox prueba la acción explícita en la interfaz; la versión persistida permite identificar de manera compacta el contenido aceptado.

### Confirmación transaccional e idempotente

La operación abrirá una transacción breve, bloqueará la fila de la solicitud activa y volverá a validar:

1. sesión vigente y asociada a la solicitud;
2. identidad verificada;
3. estado reversible permitido (`REASON_REGISTERED` o `PENDING_CONFIRMATION`);
4. motivo válido, incluida la descripción de `OTHER`;
5. al menos un certificado seleccionado, disponible y perteneciente a la solicitud;
6. consentimiento afirmativo y versión vigente.

En una primera confirmación se guardará `confirmed_at` en UTC, la versión y `CONFIRMED`, y se registrará un solo evento `CONSENT_CONFIRMED`. Una repetición con la misma solicitud ya confirmada y la misma versión devolverá el resultado persistido sin crear otro evento. Estados de revocación, abandono o finalización serán rechazados. El bloqueo de fila es preferible a agregar nuevamente una columna genérica de versión al modelo simplificado.

### Inmutabilidad después del punto de no retorno

Los casos de uso de motivo y selección comprobarán `confirmed_at`/estado antes de modificar. Se conservarán las defensas de las entidades JPA para impedir cambios accidentales durante `flush`. Confirmar no creará aún una `revocation_operation`: la solicitud `CONFIRMED` es la entrada inequívoca del siguiente incremento.

### Integración en la ruta interna

El resolutor del flujo mostrará el paso 4 cuando la solicitud tenga un motivo válido y continuará protegiendo pasos futuros. Antes de confirmar se podrá volver al paso 3 mediante el botón y el stepper. Durante la petición, checkbox, navegación y botón quedarán bloqueados. Tras el éxito, la misma vista mostrará el estado confirmado sin iniciar revocación; las recargas reconstruirán ese estado desde el backend.

Los `401` regresarán a la página pública, los `409` por versión o concurrencia permitirán recargar el resumen, y los demás errores usarán el tratamiento ciudadano ya existente sin mostrar trazas ni detalles internos.

### Migración incremental y contratos sincronizados

Flyway añadirá `consent_version VARCHAR(64) NULL` con comentario en español a `certificate_cancellation_request`. Será nullable para registros históricos; a partir de este incremento toda nueva confirmación deberá completarlo. Hibernate seguirá en `validate`. OpenAPI describirá campos, validaciones y errores, y el frontend regenerará sus tipos desde el documento real.

## Risks / Trade-offs

- [Dos pestañas confirman simultáneamente] → bloqueo transaccional, respuesta idempotente y un solo evento de auditoría.
- [El texto cambia mientras el usuario revisa] → la versión enviada debe coincidir con la versión vigente; de lo contrario se recarga el resumen.
- [Datos cambian después de cargar el resumen] → todas las reglas se revalidan dentro de la confirmación y nunca se confía en el resumen del navegador.
- [Registros antiguos confirmados no tienen versión] → la migración conserva `NULL`; no se inventa consentimiento retroactivo y la regla obligatoria aplica a confirmaciones nuevas.
- [El estado `CONFIRMED` aún no ejecuta nada] → la interfaz informa que la confirmación fue registrada y bloquea cambios; el siguiente incremento consumirá exclusivamente solicitudes en ese estado.

## Migration Plan

1. Aplicar la migración incremental nullable y validar el esquema con JPA.
2. Desplegar backend con lectura/confirmación, auditoría, validaciones y OpenAPI.
3. Sincronizar contratos y desplegar el paso 4 del frontend.
4. Verificar confirmación, recarga, doble envío y bloqueo de ediciones con MySQL.

El rollback de aplicación puede ignorar la columna nullable. No se eliminará la columna automáticamente para evitar pérdida de evidencia ya registrada.

## Open Questions

- La versión y el texto inicial serán propios de este incremento y deberán sustituirse mediante un cambio versionado si RENIEC entrega posteriormente un texto institucional definitivo.
