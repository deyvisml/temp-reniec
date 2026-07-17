## Why

La persistencia implementada en SPEC-04 modela un proceso técnico genérico y una sesión, pero no representa la solicitud ciudadana ni separa sus intentos de elegibilidad, identidad, revocación, constancia y auditoría. Corregirlo ahora, antes de almacenar información relevante o construir casos de uso, evita consolidar un esquema insuficiente y permite que el backend crezca sobre conceptos explícitos del dominio.

## What Changes

- **BREAKING** Reemplazar la migración V1 y eliminar `cancellation_process` y `cancellation_session`; el entorno local deberá recrearse porque no existe información relevante que migrar.
- Sustituir `CancellationProcessEntity` por `CertificateCancellationRequestEntity` como raíz conceptual y persistente del trámite ciudadano completo, sin interpretarlo como expediente sujeto a aprobación administrativa.
- Incorporar siete tablas de dominio: solicitud, consultas de elegibilidad, verificaciones de identidad, sesiones de solicitud, operaciones de revocación, constancias y eventos de auditoría.
- Mantener en la solicitud una vista actual del progreso: estado, ciclo de vida, elegibilidad, motivo, descripción protegida, consentimiento, confirmación, resultado final, recuperabilidad, expiración, timestamps y versión optimista.
- Diferenciar el DNI cifrado recuperable, su referencia HMAC para búsqueda y sus últimos dígitos; persistir únicamente valores ya protegidos y metadatos de versión de clave, sin implementar criptografía institucional en esta tarea.
- Registrar intentos repetibles mediante número de intento y restricciones únicas por solicitud para elegibilidad, identidad y revocación.
- Impedir atómicamente más de una solicitud activa por referencia segura de DNI mediante una clave guardiana nullable generada e índice único de MySQL.
- Modelar sesiones múltiples sin tokens en texto plano ni fingerprinting invasivo; modelar revocación con idempotencia única y resultado incierto sin reintento automático.
- Separar constancia y revocación para que una falla documental no altere una revocación confirmada.
- Añadir auditoría append-only como historial complementario, nunca como fuente de verdad ni event sourcing.
- Crear repositorios concretos y solo las consultas actuales requeridas, con relaciones unidireccionales y sin colecciones grandes cargadas desde la solicitud.
- Sustituir las pruebas de persistencia actuales por cobertura integral del nuevo esquema en MySQL Testcontainers y documentar el modelo mediante un diagrama entidad-relación.
- Mantener fuera de alcance endpoints, integraciones reales, JWT funcional, cifrado concreto, recuperación multidispositivo completa, revocación real y generación de PDF.
- Declarar este cambio como sustituto de `add-mysql-persistence-foundation`; su delta `mysql-persistence-foundation` no debe sincronizarse como contrato definitivo una vez aplicado este rediseño.

## Capabilities

### New Capabilities

- `cancellation-request-persistence-model`: Modelo MySQL completo centrado en la solicitud ciudadana, sus intentos técnicos, sesiones, revocación, constancias, auditoría, protección de datos, integridad y consultas mínimas.

### Modified Capabilities

- `backend-foundation`: La base backend conserva MySQL/Flyway/Testcontainers, pero su límite técnico permite el modelo persistente de siete entidades de dominio sin implementar casos de uso ni endpoints funcionales.

## Impact

- Migraciones: se sustituirá `V1__create_cancellation_persistence.sql` por una V1 limpia que reconstruya el esquema correcto desde una base vacía; los desarrolladores deberán reiniciar únicamente sus bases locales.
- Código: se reemplazará el paquete actual `cancellation.persistence`, sus dos entidades/repositorios y la suite `CancellationPersistenceIT` por nombres y consultas basados en solicitudes.
- Datos: no se conservará el esquema de dos tablas porque solo contiene datos efímeros de desarrollo y pruebas; no se diseñará una migración productiva ficticia.
- Dependencias y operación: se mantienen Spring Data JPA, Flyway, MySQL, Actuator y Testcontainers; no se añade otra base, mensajería, caché ni componente criptográfico.
- Documentación: `backend/README.md` se alineará con el reinicio local y se añadirá documentación ER bajo `docs`.
- API y frontend: no se crean endpoints ni se modifica `/frontend` o las referencias UI.
