## 1. Precondiciones y alcance

- [x] 1.1 Confirmar que `establish-frontend-backend-integration` fue archivado o sincronizado antes de modificar especificaciones base; pausar si sigue pendiente.
- [x] 1.2 Verificar y registrar que las bases afectadas no contienen información relevante; no reemplazar destructivamente la V1 si existen datos que deban conservarse.
- [x] 1.3 Inventariar las siete tablas, entidades, repositorios, enums, pruebas y documentación actuales, marcando cada campo como conservado, renombrado o eliminado según el diseño.

## 2. Migración MySQL simplificada

- [x] 2.1 Reemplazar la V1 por una migración limpia que conserve las siete tablas justificadas y sus relaciones, sin agregar tablas nuevas.
- [x] 2.2 Cambiar todas las claves primarias y foráneas a `BIGINT UNSIGNED AUTO_INCREMENT` y `BIGINT UNSIGNED`, respectivamente.
- [x] 2.3 Simplificar `certificate_cancellation_request` con `dni CHAR(8)`, motivo alternativo legible y estado actual directo; eliminar campos de hash, cifrado, versión de clave, últimos dígitos, ciclo de vida duplicado y guarda generada.
- [x] 2.4 Simplificar elegibilidad e identidad conservando intentos, estados, resultados, referencias, tiempos, errores y correlación; eliminar `verified_identity_hash` y datos no respaldados.
- [x] 2.5 Simplificar sesiones con una referencia opaca única, vigencia, uso e invalidación; eliminar hashes especulativos, familia de token y referencia de cliente.
- [x] 2.6 Simplificar revocaciones manteniendo idempotencia, intentos, resultado, tiempos, correlación y versión; eliminar la guarda generada y la próxima consulta no respaldada por un contrato.
- [x] 2.7 Simplificar constancias eliminando hash documental y versión de plantilla; mantener código, estado, almacenamiento, tiempos, error y relaciones.
- [x] 2.8 Reducir auditoría a evento, transición, resultado, correlación, origen y fecha; eliminar detalle técnico genérico y referencia externa duplicada.
- [x] 2.9 Conservar solo claves foráneas, unicidades y checks sencillos de integridad, y revisar cada índice contra una consulta documentada; no usar triggers, procedimientos ni columnas generadas.

## 3. Entidades y repositorios JPA

- [x] 3.1 Actualizar las siete entidades para usar identificadores `Long`, nombres directos, timestamps UTC y relaciones lazy unidireccionales desde los hijos.
- [x] 3.2 Mantener `@Version` en solicitud y revocación y retirar campos, validadores y constructores asociados a protección criptográfica inexistente.
- [x] 3.3 Revisar los enums y conservar solo estados y resultados utilizados por el modelo simplificado, sin catálogos de base de datos.
- [x] 3.4 Actualizar los siete repositorios con consultas por DNI directo, intentos recientes, sesiones vigentes, revocación actual, constancia disponible, auditoría ordenada y expiración.
- [x] 3.5 Mantener la solicitud sin colecciones bidireccionales/eager y no crear servicios vacíos, repositorios genéricos ni capas adicionales.
- [x] 3.6 Comprobar que el DNI completo y `other_reason` no aparecen en logs, errores, URLs, endpoints técnicos ni representaciones `toString`.

## 4. Pruebas de persistencia

- [x] 4.1 Actualizar las pruebas Testcontainers para comprobar Flyway desde vacío, las siete tablas simplificadas y la validación de Hibernate.
- [x] 4.2 Probar creación, lectura, actualización, búsqueda por DNI, expiración y concurrencia optimista de la solicitud.
- [x] 4.3 Probar intentos múltiples y unicidad por solicitud en elegibilidad e identidad.
- [x] 4.4 Probar varias sesiones, consulta de sesiones vigentes e invalidación sin persistir tokens o credenciales.
- [x] 4.5 Probar idempotencia y numeración de intentos de revocación, incluida la permanencia de un resultado incierto en la misma operación.
- [x] 4.6 Probar constancia asociada a solicitud/revocación, código único y falla de generación independiente del resultado de revocación.
- [x] 4.7 Probar registro y orden cronológico de auditoría, integridad de claves foráneas y rechazo de identificadores únicos duplicados.
- [x] 4.8 Mantener las pruebas técnicas de MySQL, CORS, OpenAPI y estado sin introducir endpoints funcionales ni depender de servicios externos.

## 5. Documentación transparente

- [x] 5.1 Reescribir `docs/data-model/README.md` con un diagrama real de siete tablas y una justificación breve de la cardinalidad o ciclo de vida de cada una.
- [x] 5.2 Documentar cada columna, relación, unicidad e índice y agregar consultas SQL sencillas para inspeccionar DNI, solicitud e historiales relacionados.
- [x] 5.3 Documentar expresamente la decisión de DNI legible, su exclusión de logs/errores/URLs y los datos que continúan prohibidos en MySQL.
- [x] 5.4 Actualizar README y decisiones relacionadas para retirar referencias a UUID binarios, cifrado inexistente, columnas generadas y campos eliminados.
- [x] 5.5 Explicar la diferencia entre solicitud ciudadana, operación técnica de revocación y constancia, y dejar claro que la auditoría no es event sourcing.

## 6. Verificación y cierre

- [x] 6.1 Ejecutar `mvn test` y `mvn clean verify` con MySQL Testcontainers y confirmar que todas las pruebas pasan.
- [x] 6.2 Validar el arranque local con Compose, Flyway y Hibernate e inspeccionar el esquema mediante SQL sin exponer DNI en el endpoint técnico.
- [x] 6.3 Comparar el esquema final con el inventario de campos y confirmar que cada tabla y cada índice tiene una justificación documentada.
- [x] 6.4 Detener procesos temporales conservando el volumen cuando corresponda, retirar logs privados y verificar que no se modificó frontend ni se implementó flujo ciudadano.
- [x] 6.5 Ejecutar validación OpenSpec estricta y confirmar que todas las tareas del cambio están completas antes de archivarlo.
