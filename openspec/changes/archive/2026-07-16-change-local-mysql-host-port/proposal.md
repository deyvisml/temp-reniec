## Why

El MySQL local de Docker Compose publica actualmente el puerto `3306`, el mismo que normalmente utiliza una instalación nativa de MySQL. Aunque esa instalación esté detenida hoy, iniciarla produciría un conflicto y bloquearía uno de los dos servicios.

## What Changes

- Cambiar el puerto MySQL publicado por defecto en el equipo de `3306` a `3307`, manteniendo el puerto interno del contenedor en `3306`.
- Mantener `DB_PORT` configurable en `backend/.env`, de modo que cada desarrollador pueda elegir otro puerto libre sin modificar archivos versionados.
- Alinear la plantilla `.env`, el valor local predeterminado de Spring Boot y la documentación con el puerto `3307`.
- Documentar claramente la diferencia entre el puerto del host y el puerto interno del contenedor, además del procedimiento para resolver otro conflicto local.
- Verificar que Compose publica `3307:3306` y que el backend inicia, ejecuta Flyway y responde salud usando el nuevo puerto.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `backend-foundation`: La configuración y operación local deberán usar `3307` como puerto MySQL predeterminado en el host, conservando `DB_PORT` como anulación local.

## Impact

El cambio afecta `backend/.env.example`, el valor predeterminado de `DB_PORT` en `application-local.yml` y `backend/README.md`. `backend/compose.yaml` ya publica `${DB_PORT}:3306`, por lo que no requiere cambiar su estructura. No se modifican el esquema, las migraciones, las dependencias, las APIs ni el comportamiento funcional.

Este cambio es el sucesor correctivo de `add-compose-local-mysql-environment`; ese cambio debe permanecer como línea base y archivarse antes que este para conservar el orden de especificaciones.
