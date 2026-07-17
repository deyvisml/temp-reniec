## Why

El arranque local del backend depende hoy de una instalación y configuración manual de MySQL y de variables de entorno definidas en cada sesión. Un entorno local reproducible con Docker Compose y un archivo `.env` ignorado permitirá comenzar a desarrollar con una preparación única, sin convertir las aplicaciones en contenedores ni introducir configuración productiva.

## What Changes

- Añadir `backend/compose.yaml` con un único servicio MySQL 8.4, volumen persistente y healthcheck.
- Añadir `backend/.env.example` con valores exclusivamente locales y excluir `backend/.env` de Git.
- Permitir que el perfil Spring Boot `local` importe opcionalmente `backend/.env`, conservando la posibilidad de sobrescribir sus valores mediante el entorno del sistema.
- Mantener Flyway como propietario del esquema y ejecutarlo automáticamente durante el arranque del backend contra la base local.
- Actualizar la documentación con la preparación inicial, inicio, detención, reinicio destructivo, validación de Compose y comprobación del backend y su endpoint de salud.
- Mantener backend y frontend fuera de Docker Compose, sin Dockerfiles, scripts PowerShell, credenciales productivas ni cambios funcionales.

## Capabilities

### New Capabilities

- `local-mysql-development-environment`: Entorno MySQL local reproducible mediante Docker Compose, configuración `.env` compartida y flujo documentado de operación y validación.

### Modified Capabilities

- `backend-foundation`: El perfil local y la documentación del backend pasan de requerir una base y variables configuradas manualmente a admitir el archivo `.env` local y el MySQL gestionado por Compose.

## Impact

El cambio afecta únicamente la infraestructura de desarrollo local y la documentación: `backend/compose.yaml`, `backend/.env.example`, `backend/.gitignore`, `backend/src/main/resources/application-local.yml` y `backend/README.md`. No cambia APIs, entidades, migraciones funcionales, dependencias Maven, frontend ni configuración de despliegue.
