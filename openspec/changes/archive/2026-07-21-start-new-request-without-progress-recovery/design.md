## Context

El inicio actual busca por DNI una solicitud en estado activo, devuelve su `requestId` y evita una nueva consulta cuando encuentra progreso elegible. El DTO expone `reused`, OpenAPI denomina la operación “iniciar o recuperar” y la documentación promete recuperación indefinida desde cualquier navegador.

Ese comportamiento entra en conflicto con el dominio actualizado: una solicitud finalizada puede haber cancelado solo algunos certificados y un nuevo ingreso debe consultar cuáles siguen vigentes para permitir otra selección. Una solicitud anterior debe seguir almacenada para auditoría y constancia, pero no debe convertirse automáticamente en la sesión o pantalla actual del ciudadano.

La implementación todavía no tiene JWT ni una tabla de sesiones. El coordinador separa la preparación transaccional, la llamada externa y la finalización transaccional, por lo que el cambio debe mantener consistencia frente a doble envío, consultas en curso y respuestas tardías.

## Goals / Non-Goals

**Goals:**

- Hacer que cada inicio desde la página principal cree una solicitud nueva y una consulta nueva.
- Conservar íntegramente el historial sin restaurarlo como contexto activo.
- Sustituir de forma transaccional solicitudes previas que todavía no fueron confirmadas.
- Evitar una segunda revocación mientras una operación confirmada está en curso o tiene resultado incierto.
- Eliminar `reused` y toda promesa de recuperación del contrato, documentación y pruebas.
- Mantener la base sencilla, sin migración estructural ni infraestructura de sesiones.

**Non-Goals:**

- Eliminar solicitudes, certificados, selecciones, operaciones, resultados o constancias históricas.
- Implementar una pantalla para consultar o descargar constancias anteriores.
- Implementar JWT, refresh tokens, recuperación multidispositivo o almacenamiento del progreso en el navegador.
- Cambiar el contrato institucional de consulta, ID Perú o revocación.
- Resolver todavía la navegación de los pasos posteriores al inicio.

## Decisions

### 1. Un envío nuevo desde inicio siempre crea una solicitud

`POST /api/v1/cancellation-requests` dejará de ser “crear o recuperar” y pasará a ser únicamente “iniciar y consultar”. Nunca devolverá un `requestId` histórico ni omitirá la consulta porque exista una solicitud elegible anterior. La nueva solicitud tendrá su propio intento número 1 y almacenará una lista fresca de certificados cuando esa integración sea actualizada.

Alternativa descartada: recuperar únicamente solicitudes incompletas y crear una nueva después de completadas. Mantendría el concepto que el usuario rechazó y podría reabrir una selección o motivo que ya no representa su intención actual.

### 2. El progreso solo describe el trámite actual, no una sesión recuperable

`request_status` seguirá siendo necesario para controlar transiciones y conservar trazabilidad dentro de una solicitud. No se utilizará para decidir qué pantalla restaurar cuando el ciudadano vuelve a la página de inicio. Continuar inmediatamente con el `requestId` recién emitido forma parte del mismo trámite; volver a ingresar el DNI constituye otro trámite.

No se guardará el DNI ni el progreso en `localStorage` o `sessionStorage`, y no se añadirá `cancellation_request_session`.

### 3. Solicitudes anteriores no confirmadas se sustituyen con `ABANDONED`

Antes de crear la nueva solicitud, el backend bloqueará la solicitud más reciente del DNI. Si está en un estado previo a la confirmación y no tiene una operación externa irreversible en curso, la marcará `ABANDONED` y registrará el evento mínimo correspondiente cuando aplique. Después creará una fila nueva; no reutilizará intentos, certificados ni selecciones anteriores.

Se reutiliza `ABANDONED` porque ya expresa que ese trámite no continuará. No se añadirá `SUPERSEDED`, una columna de sesión ni una relación entre solicitudes sin un requisito adicional confirmado.

### 4. Operaciones en curso o inciertas bloquean un nuevo inicio sin recuperarse

Los estados `CHECKING_ELIGIBILITY`, `CONFIRMED`, `REVOCATION_IN_PROGRESS`, `OUTCOME_UNKNOWN` y `REVOCATION_OUTCOME_UNKNOWN` requieren tratamiento conservador:

- Una elegibilidad realmente en curso devuelve el conflicto existente para impedir duplicar la llamada.
- Si superó el umbral de intento obsoleto, el intento se cierra como error, la solicitud se abandona y se crea otra solicitud con un intento nuevo.
- Una solicitud confirmada, en revocación o con resultado incierto bloquea temporalmente el inicio con un error estable y genérico. No devuelve su `requestId`, paso, certificados ni constancia.
- Los resultados terminales permiten un inicio nuevo aunque la solicitud histórica y su constancia permanezcan almacenadas.

Esta excepción no es recuperación: protege la idempotencia y evita revocar nuevamente certificados cuyo resultado todavía no está confirmado.

### 5. Serialización por DNI y protección frente a respuestas tardías

La decisión se realizará en una transacción con bloqueo pesimista sobre la solicitud más reciente del DNI. Los envíos concurrentes deberán producir como máximo una solicitud nueva con una consulta activa; el otro recibirá un conflicto controlado.

La finalización de elegibilidad verificará que el intento siga `SUBMITTED` y que su solicitud continúe en el estado esperado. Una respuesta externa que llegue después de que el intento fue declarado obsoleto no podrá reactivar ni modificar la solicitud abandonada.

Alternativa descartada: índice único permanente por DNI. Impediría conservar solicitudes históricas y no representa la regla temporal.

### 6. `reused` se elimina de extremo a extremo

El campo siempre sería falso y preservaría una semántica retirada. Se eliminará del record Java, anotaciones OpenAPI, JSON versionado, tipos TypeScript y fixtures. La operación y el tag Swagger dejarán de mencionar recuperación. Este cambio es incompatible a nivel de contrato, pero el frontend y backend del mismo repositorio se actualizarán juntos.

### 7. La constancia histórica no se reabre automáticamente

Cerrar el flujo y volver al inicio nunca navegará a `RECEIPT_AVAILABLE` ni ofrecerá otra vez la descarga anterior. La constancia permanece en MySQL para evidencia. Si posteriormente se requiere reimpresión, se diseñará un caso de uso separado con autenticación explícita y reglas propias.

## Risks / Trade-offs

- [El ciudadano abandona accidentalmente un trámite previo a la confirmación] → La operación aún es reversible y el nuevo inicio representa una intención explícita; el historial permanece disponible para soporte y auditoría.
- [Dos inicios simultáneos compiten por el mismo DNI] → Bloqueo transaccional, estado `CHECKING_ELIGIBILITY` y manejo de conflictos garantizan una sola consulta activa.
- [Una respuesta externa tardía intenta finalizar una solicitud abandonada] → Validar estado del intento y solicitud antes de aplicar el resultado.
- [Una revocación incierta impide temporalmente empezar otra] → Priorizar idempotencia; responder con mensaje genérico y correlación sin recuperar detalles anteriores.
- [Eliminar `reused` rompe consumidores antiguos] → Regenerar y verificar conjuntamente OpenAPI y tipos TypeScript; no existe un consumidor externo oficial confirmado.
- [El historial crecerá con cada nuevo ingreso] → Es comportamiento intencional y el volumen del MVP es manejable; retención será una decisión posterior.

## Migration Plan

1. Archivar o sincronizar primero los cambios completados de contexto v2 y certificados seleccionables para que esta corrección se aplique sobre el modelo vigente de ocho tablas.
2. Actualizar contexto, decisiones técnicas, especificaciones y documentación del modelo eliminando recuperación automática.
3. Cambiar el coordinador y repositorio para clasificar la solicitud anterior, abandonarla o bloquear el inicio y crear una solicitud nueva.
4. Proteger la finalización contra intentos obsoletos o solicitudes abandonadas.
5. Eliminar `reused` del DTO y sincronizar OpenAPI, tipos TypeScript y pruebas.
6. Ejecutar pruebas rápidas, Testcontainers, integración frontend-backend y validación estricta de OpenSpec.

No se requiere Flyway V3. El rollback de código puede restaurar temporalmente el comportamiento anterior porque el esquema no cambia, aunque las solicitudes creadas durante el nuevo comportamiento permanecerán como historial válido.

## Open Questions

- El texto ciudadano exacto para un trámite confirmado todavía en curso deberá validarse al implementar la pantalla correspondiente; el contrato utilizará un código estable sin revelar detalles.
- La consulta histórica o reimpresión de constancias, si llega a requerirse, necesita una especificación independiente y autenticación confirmada.
