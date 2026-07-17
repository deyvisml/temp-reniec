## Why

El backend ya dispone de una base web ejecutable, pero todavía no puede conservar el progreso ni representar de forma durable un proceso de cancelación. Se necesita incorporar MySQL y una persistencia inicial mínima antes de desarrollar los casos de uso ciudadanos, manteniendo protegida la referencia al DNI y evitando un modelo acoplado a pantallas o integraciones aún no definidas.

## What Changes

- Incorporar Spring Data JPA, Flyway y el driver oficial de MySQL usando las versiones gestionadas por Spring Boot 4.1.0.
- Externalizar host, puerto, base, usuario y contraseña de MySQL, con configuración diferenciada para desarrollo local y pruebas y sin credenciales en el repositorio.
- Crear una migración reproducible para una tabla principal `cancellation_process` y una tabla `cancellation_session` justificada por la relación uno-a-muchos y la futura recuperación desde otro dispositivo.
- Representar el proceso mediante un UUID interno, referencia de búsqueda del DNI no reversible, dígitos enmascarables, estado extensible, vigencia, indicador activo, timestamps y versión de concurrencia; el estado del proceso representará también el resultado mínimo de elegibilidad para evitar un campo duplicado.
- Persistir sesiones mediante una referencia técnica irreversible, vigencia e invalidación, sin emitir JWT, cookies, refresh tokens ni implementar recuperación.
- Añadir repositorios JPA concretos para guardar y recuperar procesos, localizar procesos vigentes y gestionar las referencias de sesión, sin servicios, endpoints o capas especulativas.
- Validar el esquema con Flyway y `ddl-auto=validate`, mantener los detalles de salud ocultos e incorporar el estado de MySQL al health agregado.
- Añadir pruebas de integración sobre MySQL real efímero con Testcontainers para migraciones, persistencia, consultas, integridad, expiración y bloqueo optimista.
- Actualizar la documentación local del backend con preparación de MySQL, variables, migraciones y requisitos de las pruebas de persistencia.
- Mantener fuera de alcance toda funcionalidad ciudadana, autenticación, integración externa, auditoría funcional y modificación del frontend.

## Capabilities

### New Capabilities

- `mysql-persistence-foundation`: Configuración MySQL, esquema inicial versionado, modelo mínimo de proceso y sesiones, repositorios, integridad, concurrencia y pruebas de persistencia.

### Modified Capabilities

- `backend-foundation`: El backend deja de ser independiente de base de datos al ejecutarse normalmente; amplía sus dependencias gestionadas, configuración, health, pruebas y documentación para requerir MySQL o un contenedor efímero según el ambiente.

## Impact

- Código y configuración afectados: `/backend/pom.xml`, perfiles Spring, documentación y nuevos paquetes de persistencia dentro del paquete institucional.
- Esquema afectado: nuevas migraciones en `backend/src/main/resources/db/migration` para dos tablas relacionadas e índices mínimos.
- Operación local: el perfil `local` requerirá una instancia MySQL y credenciales externas; el health seguirá exponiendo únicamente información agregada y no sensible.
- Pruebas: las pruebas de integración requerirán un runtime compatible con Testcontainers, pero no una instalación manual de MySQL ni servicios externos.
- API y frontend: no se crean endpoints ni llamadas funcionales y `/frontend` no se modifica.
