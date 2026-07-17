## Context

El entorno local Compose ya desacopla el puerto publicado mediante `DB_PORT`, pero la plantilla lo fija en `3306`. Ese es también el puerto convencional de una instalación nativa de MySQL, por lo que ambos servicios no pueden iniciarse simultáneamente en el mismo equipo. El contenedor debe seguir escuchando en su puerto estándar; el conflicto existe únicamente en la interfaz del host.

Este es un ajuste correctivo posterior a `add-compose-local-mysql-environment`. No cambia la persistencia, el volumen ni las credenciales.

## Goals / Non-Goals

**Goals:**

- Reservar `3307` como puerto host predeterminado para el MySQL administrado por Compose.
- Mantener `3306` como puerto interno del contenedor.
- Hacer que Compose y Spring Boot utilicen el mismo `DB_PORT` desde `.env`.
- Permitir una anulación sencilla si `3307` también está ocupado.
- Verificar que Flyway y el health endpoint funcionan con el nuevo mapeo.

**Non-Goals:**

- Reconfigurar, iniciar, detener o administrar el MySQL instalado en el sistema operativo.
- Detectar y escoger automáticamente un puerto en cada arranque.
- Cambiar el puerto interno de MySQL, el volumen, el esquema o las migraciones.
- Añadir scripts, nuevas dependencias o configuración productiva.

## Decisions

### Publicar `3307:3306` por defecto

`backend/.env.example` usará `DB_PORT=3307`; `backend/compose.yaml` conservará el mapeo `${DB_PORT}:3306`. Así, el MySQL nativo puede ocupar `localhost:3306` y Compose ocupa `localhost:3307`, mientras el servicio dentro del contenedor mantiene su configuración estándar.

Se elige `3307` por ser el puerto adyacente y fácil de reconocer para una segunda instancia local. Usar un puerto aleatorio evitaría algunas colisiones, pero impediría que Spring conociera de forma estable el destino sin coordinación adicional.

### Alinear el fallback de Spring con la plantilla

El datasource del perfil `local` cambiará su fallback de `${DB_PORT:3306}` a `${DB_PORT:3307}`. Normalmente `.env` suministrará el valor, pero el fallback coherente evita comportamientos distintos si se proporcionan usuario y contraseña mediante el entorno y se omite `DB_PORT`.

Quien desee conectar el backend al MySQL nativo deberá establecer explícitamente `DB_PORT=3306`; no habrá detección automática del tipo de servidor.

### Conservar la personalización en `.env`

Si `3307` también está ocupado, el desarrollador elegirá otro puerto libre —por ejemplo `3308`— modificando únicamente su `backend/.env`. Compose y Spring leerán el mismo valor, por lo que no será necesario editar YAML versionado.

La documentación mostrará cómo comprobar el mapeo con `docker compose ps`. No se añadirá lógica automática de selección porque un puerto variable haría menos predecible la depuración y podría cambiar entre reinicios.

### No recrear el volumen por un cambio de puerto

El puerto publicado es independiente del volumen nombrado. El procedimiento de actualización será detener el contenedor, actualizar o recrear `.env` y volver a iniciarlo; los datos se conservarán. `docker compose down -v` no formará parte de esta migración.

## Risks / Trade-offs

- **El puerto 3307 también está ocupado** → documentar la modificación de `DB_PORT` en el `.env` privado y verificar el puerto resultante con Compose.
- **Un `.env` existente conserva 3306** → advertir que los archivos ignorados no se actualizan al cambiar `.env.example`; el desarrollador debe cambiar una línea o regenerar el archivo conscientemente.
- **El desarrollador pretende usar el MySQL nativo** → documentar `DB_PORT=3306` como configuración explícita para ese caso, sin iniciar Compose simultáneamente sobre el mismo puerto.
- **Spring y Compose usan valores distintos** → ejecutar ambos desde `/backend` con el mismo `.env` y validar el mapeo antes del arranque.

## Migration Plan

1. Cambiar `DB_PORT` a `3307` en `.env.example` y en el fallback de `application-local.yml`.
2. Actualizar el README con la distinción host/contenedor y la personalización del puerto.
3. Si existe un `.env` local, detener Compose y cambiar manualmente `DB_PORT=3307` o copiar nuevamente la plantilla después de revisar cualquier personalización.
4. Validar que Compose resuelve `3307:3306`, iniciar MySQL y comprobar que Spring Boot, Flyway y Actuator funcionan.
5. Detener el entorno normalmente, conservando el volumen.

La reversión consiste en volver a `DB_PORT=3306` únicamente cuando ese puerto esté libre. No requiere modificar ni eliminar datos.

## Open Questions

No quedan preguntas abiertas. `3307` será el valor recomendado, no una reserva global ni productiva.
