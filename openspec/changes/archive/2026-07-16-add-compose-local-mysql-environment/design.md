## Context

El backend ya usa MySQL, Flyway y un perfil `local`, pero el desarrollador debe crear la base manualmente y exportar `DB_USERNAME` y `DB_PASSWORD` en cada terminal. Testcontainers resuelve las pruebas de integración, no la ejecución interactiva del backend. El objetivo es ofrecer una base local persistente y reproducible sin contenerizar Spring Boot ni el frontend.

La configuración debe seguir siendo segura por defecto: el repositorio solo contendrá credenciales explícitamente locales, `backend/.env` no se versionará y cualquier entorno real seguirá proporcionando sus valores fuera de este mecanismo.

## Goals / Non-Goals

**Goals:**

- Levantar MySQL 8.4 con un único comando desde `/backend`.
- Reutilizar las mismas variables `DB_*` en Docker Compose y Spring Boot después de copiar una sola vez `.env.example` a `.env`.
- Conservar datos entre reinicios normales y ofrecer un reinicio destructivo explícito para bases locales desechables.
- Esperar una señal de salud de MySQL y comprobar que Flyway construye o valida el esquema al arrancar Spring Boot con el perfil `local`.
- Documentar y validar un recorrido corto desde un checkout limpio hasta `/actuator/health` en estado `UP`.

**Non-Goals:**

- Contenerizar backend o frontend, crear Dockerfiles o scripts de automatización.
- Definir credenciales, redes, volúmenes o políticas propias de producción.
- Cambiar el modelo de datos, las migraciones, las dependencias Maven o el comportamiento funcional.
- Sustituir Testcontainers en las pruebas de persistencia.

## Decisions

### Un Compose local con un solo servicio MySQL

`backend/compose.yaml` declarará únicamente un servicio `mysql` basado en `mysql:8.4`. Publicará el puerto local configurable hacia `3306`, montará un volumen nombrado en `/var/lib/mysql` y definirá un healthcheck con `mysqladmin ping` usando la contraseña root disponible dentro del contenedor.

Se elige Compose porque expresa de forma portable la versión, configuración, persistencia y salud de MySQL. No se incluirá Spring Boot en el archivo: ejecutarlo con Maven mantiene el ciclo de desarrollo y depuración actual. Una instalación manual de MySQL seguirá siendo posible mediante las mismas variables, pero dejará de ser el recorrido recomendado.

### `.env.example` versionado y `.env` privado compartido

`backend/.env.example` contendrá `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD` con valores identificados como locales. El desarrollador lo copiará una vez a `backend/.env`; `backend/.gitignore` excluirá exactamente `.env`.

Docker Compose carga automáticamente el `.env` del directorio del proyecto. Spring Boot lo leerá solo con el perfil `local` mediante `spring.config.import: optional:file:.env[.properties]`. La importación es opcional para no impedir el uso de variables del sistema o de una base instalada por otro mecanismo. Las variables del entorno del proceso conservarán mayor precedencia que el archivo importado.

No se añade una librería dotenv: Spring Config Data ya interpreta el formato de propiedades y evita una dependencia innecesaria. El README exigirá ejecutar los comandos desde `/backend`, donde la ruta relativa resulta predecible.

### Flyway conserva la propiedad del esquema

No se modificará la migración existente ni se introducirá un contenedor separado de migraciones. Al iniciar Spring Boot, la autoconfiguración existente de Flyway conectará a MySQL, aplicará la V1 desde una base vacía y luego Hibernate validará el resultado. Si MySQL todavía no está disponible o la migración falla, el backend no deberá ocultar el error ni crear un esquema alternativo.

### Persistencia y reinicio son operaciones diferentes

`docker compose down` detendrá y eliminará el contenedor y la red, pero conservará el volumen. `docker compose down -v` será el único reinicio documentado que elimina datos; deberá advertirse que solo se use con información local desechable. El inicio recomendado será `docker compose up -d --wait`, aprovechando el healthcheck antes de arrancar Spring Boot.

### Validación en dos niveles

La estructura se validará con `docker compose --env-file .env.example config`, sin depender de un `.env` privado. La comprobación operativa iniciará MySQL, ejecutará Spring Boot con `local`, confirmará la ejecución de Flyway y consultará `/actuator/health`. Las pruebas Maven existentes con Testcontainers continuarán verificando el esquema de forma aislada.

## Risks / Trade-offs

- **El puerto 3306 ya está ocupado** → `DB_PORT` seguirá siendo configurable en `.env`; Compose publicará ese valor y Spring usará el mismo.
- **La importación relativa no encuentra `.env` al ejecutar desde otro directorio** → la documentación fija `/backend` como directorio de trabajo y conserva variables del sistema como alternativa.
- **Las credenciales de ejemplo se confunden con credenciales reales** → los nombres y comentarios las identificarán como locales, `.env` estará ignorado y la documentación prohibirá reutilizarlas fuera del equipo de desarrollo.
- **Un `down -v` elimina datos por accidente** → el comando se separará del apagado normal y se marcará explícitamente como destructivo y exclusivo para datos locales desechables.
- **El healthcheck está saludable antes de que Spring pueda migrar** → la salud de MySQL solo confirma disponibilidad del servidor; el arranque y health de Spring serán la comprobación independiente de Flyway y del esquema.

## Migration Plan

1. Añadir el archivo Compose, la plantilla de variables y la exclusión de `.env`.
2. Incorporar la importación opcional al perfil `local` y actualizar el README.
3. Validar la interpolación con `.env.example`.
4. Copiar la plantilla a `.env`, iniciar MySQL y, si existe una V1 local obsoleta, eliminar únicamente el volumen local desechable siguiendo la advertencia documentada.
5. Arrancar Spring Boot con `local` y comprobar Flyway y `/actuator/health`.

La reversión consiste en detener Compose, retirar los nuevos archivos y restaurar las instrucciones manuales. El volumen puede conservarse durante la reversión o eliminarse explícitamente si solo contiene datos desechables.

## Open Questions

No quedan decisiones abiertas para este cambio. La configuración de producción y una posible separación futura entre credenciales de migración y runtime permanecen fuera de alcance.
