## Context

El esquema vigente tiene siete tablas. La solicitud principal contiene `public_reference`, `consent_version`, `recoverable_until` y `version`; `revocation_operation` también tiene `version`; y existe `cancellation_request_session` para una interpretación anterior de recuperación multidispositivo. La funcionalidad implementada hasta ahora solo necesita identificar la solicitud creada, conservar su `request_status`, evitar duplicados por DNI y registrar intentos de elegibilidad.

La intención aclarada es más simple: recuperar progreso significa encontrar la solicitud vigente del ciudadano y continuar desde el estado persistido, no conservar una colección de sesiones por navegador o dispositivo. JWT podrá resolver la sesión HTTP cuando se implemente, pero esa decisión no obliga a persistir sesiones en MySQL.

## Goals / Non-Goals

**Goals:**

- Dejar un esquema final de seis tablas fácil de consultar y explicar.
- Eliminar columnas que no tienen una necesidad funcional vigente.
- Recuperar progreso con `dni` y `request_status` en la solicitud principal, sin vencimiento temporal para el MVP.
- Mantener consistencia transaccional sin columnas de versión genéricas.
- Actualizar de extremo a extremo el identificador devuelto al frontend.
- Preservar la información funcional relevante durante la simplificación.

**Non-Goals:**

- Implementar JWT, cookies, refresh tokens o recuperación funcional completa.
- Implementar ID Perú, motivo, confirmación, revocación o constancia.
- Eliminar las tablas de intentos, revocación, constancia o auditoría que tienen responsabilidades diferenciadas.
- Cambiar las reglas de validación del DNI o los resultados de elegibilidad.
- Añadir Redis, almacenamiento de sesión alternativo o una tabla sustituta.

## Decisions

### 1. La solicitud se recupera directamente por DNI y estado

La recuperación buscará la solicitud más reciente del DNI cuyo estado todavía no sea final. No se comprobará una fecha límite: mientras el trámite permanezca sin finalizar, siempre podrá retomarse. `request_status` indica el paso alcanzado; no se añadirá `current_step` porque duplicaría la misma información.

Se descarta `cancellation_request_session` porque su historial de sesiones, invalidaciones y últimos usos no es necesario para encontrar el progreso. También se descarta reemplazarla por otra tabla o por datos de dispositivo.

### 2. El MVP no tendrá expiración de solicitudes

Se eliminan `recoverable_until` y `expires_at`, y no se realizará una transición automática a `EXPIRED`. Una solicitud sin finalizar siempre podrá retomarse. Las solicitudes completadas o cerradas permanecerán como historial y no se confundirán con progreso pendiente.

Se descarta conservar una fecha de expiración “para el futuro” porque no existe una regla del MVP que la utilice. Si posteriormente aparece una política real de caducidad o retención, deberá proponerse con sus criterios funcionales concretos.

### 3. La confirmación conserva solo el hecho confirmado

Se elimina `consent_version` y se mantiene `confirmed_at`. El sistema seguirá exigiendo confirmación expresa en su futura implementación, pero no almacenará una versión documental hasta que exista un texto institucional versionado y una obligación real de conservar esa versión.

### 4. El identificador numérico reemplaza al UUID público

Se elimina `public_reference`. `POST /api/v1/cancellation-requests` devolverá `requestId`, correspondiente al `id` numérico de la solicitud, y el frontend lo utilizará para la transición preparada. El DNI seguirá fuera de la URL.

El identificador no será una credencial: ningún endpoint sensible podrá autorizar una operación únicamente por conocer `requestId`. Se acepta que el valor sea secuencial a cambio de reducir duplicación; la protección real deberá provenir de la verificación de identidad y la autorización futura.

Las alternativas descartadas son conservar el UUID, introducir otro token opaco o guardar la referencia en almacenamiento del navegador, porque todas reintroducen infraestructura que el cambio busca retirar.

### 5. La concurrencia se controla donde ocurre

Se eliminan las columnas `version` y las anotaciones `@Version` de solicitud y revocación. El inicio actual ya serializa la creación o recuperación mediante transacciones y bloqueos de base de datos. Las futuras transiciones sensibles deberán usar bloqueos explícitos o actualizaciones condicionales sobre el estado esperado, con pruebas concurrentes, en lugar de agregar una columna transversal preventiva.

### 6. La migración preferida será una línea base limpia en esta etapa

Antes de editar migraciones se comprobará si existe información no desechable. Si solo existen datos locales o ficticios, se consolidará la estructura final en `V1`, se eliminará la migración dedicada a `public_reference` y se documentará recrear el volumen local para evitar un historial que crea y elimina inmediatamente los mismos elementos.

Si se descubre información relevante, la consolidación destructiva se detendrá y se utilizará una migración hacia adelante que elimine la tabla y columnas acordadas sin afectar las otras seis tablas. No se fingirá preservar el contenido de sesiones, versiones o referencias que este cambio declara obsoleto.

### 7. La decisión de JWT queda desacoplada del modelo

Un JWT futuro podrá ser autocontenido y de corta duración o usar el mecanismo mínimo que se defina en su propia tarea. Este cambio no presupone refresh tokens persistentes. Después de volver a verificar al ciudadano, el backend podrá localizar la solicitud vigente por DNI y emitir la autorización correspondiente sin crear una fila por dispositivo.

## Risks / Trade-offs

- [Un identificador secuencial puede enumerarse] → No devolver datos personales ni autorizar acciones sensibles por `requestId`; exigir verificación y autorización en las siguientes etapas.
- [Sin `consent_version` no se demuestra qué texto se aceptó] → Registrar `confirmed_at` ahora y añadir versionado solo cuando exista un texto institucional y una obligación confirmada.
- [Sin `@Version` una actualización concurrente podría perderse] → Mantener transacciones breves, bloqueo explícito en los casos demostrados y pruebas de concurrencia sobre cada transición sensible.
- [Una solicitud abandonada podría permanecer pendiente indefinidamente] → Se acepta para el MVP; una futura política de abandono o retención deberá justificarse con reglas institucionales reales.
- [Reescribir migraciones exige reiniciar bases locales ya migradas] → Verificar que los datos sean desechables, documentar `docker compose down -v` y usar una migración hacia adelante si existe información relevante.
- [Eliminar sesiones limita revocación individual por dispositivo] → Se acepta porque ese comportamiento no forma parte del requisito aclarado; cualquier necesidad futura deberá justificarse por separado.

## Migration Plan

1. Inspeccionar las tablas locales y confirmar si contienen solamente datos ficticios o desechables.
2. Consolidar Flyway desde una base vacía en seis tablas, o crear la migración hacia adelante si aparecen datos relevantes.
3. Retirar entidad y repositorio de sesiones y limpiar sus pruebas.
4. Retirar los cuatro conceptos del modelo JPA y adaptar constructores, validaciones y consultas.
5. Cambiar el DTO de `publicReference` a `requestId`, actualizar OpenAPI y regenerar TypeScript.
6. Adaptar navegación, integración y pruebas para utilizar el identificador numérico.
7. Actualizar documentación y decisiones técnicas.
8. Ejecutar migraciones desde cero, pruebas MySQL, backend, frontend, contrato e integración real.

El rollback previo a entornos con datos consiste en restaurar la versión anterior y su esquema. Una vez eliminados datos de las columnas o sesiones no se promete reconstruirlos, porque se consideran deliberadamente innecesarios.

## Open Questions

No quedan preguntas que bloqueen esta propuesta. El mecanismo concreto de JWT y autorización se decidirá en su tarea correspondiente sin reintroducir automáticamente una tabla de sesiones.
