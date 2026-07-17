## Context

El backend ya dispone de Spring MVC, un formato `ApiError`, correlación `X-Correlation-ID`, Actuator, Flyway y MySQL. El frontend ya dispone de Next.js, variables de entorno y un `requestJson<T>` basado en `fetch`, pero solo funciona del lado servidor, no tiene timeout, no envía correlación y no realiza llamadas reales. La página inicial declara que la integración queda pendiente.

SPEC-05 debe convertir esas piezas en una base contractual reutilizable sin comenzar el flujo ciudadano. La integración debe ser observable desde el navegador, verificable contra MySQL y suficientemente estricta para detectar divergencias entre Java y TypeScript.

## Goals / Non-Goals

**Goals:**

- Definir una convención API versionada y un endpoint técnico que compruebe backend y MySQL.
- Permitir comunicación directa y segura desde `http://localhost:3000` al backend local.
- Consolidar URL, JSON, timeout, correlación y errores en un único cliente frontend.
- Publicar un contrato OpenAPI generado desde el código backend y derivar de él los tipos TypeScript.
- Mantener una comprobación visual temporal y accesible, separada del futuro flujo ciudadano.
- Probar por separado las unidades aisladas y la integración real de las tres capas.

**Non-Goals:**

- Implementar DNI, elegibilidad funcional, solicitudes, JWT, refresh token, recuperación, ID Perú, motivos, confirmación, revocación, constancia o administración.
- Crear un SDK completo, interceptores, reintentos automáticos, gestor de estado o validación runtime general del contrato.
- Introducir un gateway, BFF, proxy de Next.js, microservicio o nueva base de datos.
- Definir CORS, OpenAPI o URLs productivas definitivas.

## Decisions

### API de aplicación bajo `/api/v1`

Las rutas funcionales futuras usarán `/api/v1`. Esta tarea añadirá únicamente `GET /api/v1/system/status`, con una respuesta JSON estable:

```json
{
  "status": "UP",
  "database": "UP",
  "timestamp": "2026-07-16T23:00:00Z"
}
```

El controlador delegará en un servicio técnico que ejecute `SELECT 1` mediante `JdbcTemplate`, ya disponible transitivamente por Spring Data JPA. Esto demuestra una comunicación real con MySQL sin leer tablas del dominio ni añadir una dependencia directa. No se reutilizará el JSON de Actuator porque su contrato es operacional, no el contrato versionado que consumirá el frontend.

Si la comprobación falla, se responderá `503` con el `ApiError` común, código `DEPENDENCY_UNAVAILABLE`, mensaje público genérico, ruta, UTC y correlación. No se expondrán JDBC URL, consulta, excepción, credenciales, versión del servidor ni detalles del esquema.

### CORS explícito y configurable

Una configuración compartida limitará CORS a `/api/**`. `CORS_ALLOWED_ORIGINS` aceptará una lista explícita; el perfil local usará `http://localhost:3000`. Se permitirán `GET`, `POST` y `OPTIONS`, los headers `Accept`, `Content-Type` y `X-Correlation-ID`, credenciales futuras y la exposición de `X-Correlation-ID`. No se usarán `*`, patrones amplios ni reflexión del origen recibido.

Se incluye `POST` porque la siguiente consulta de certificados probablemente lo necesitará para no colocar DNI en URLs o logs, pero no se crea aún ese endpoint. Nuevos métodos se agregarán cuando exista un caso real.

### Un cliente `fetch` con fallos explícitos y sin reintentos

`requestJson` seguirá usando la API nativa. Resolverá `BACKEND_URL` en servidor y `NEXT_PUBLIC_BACKEND_URL` en navegador, ambas con valor local `http://localhost:8080`. La URL pública no contendrá secretos.

Cada solicitud generará un UUID de correlación si el llamador no proporciona uno, lo enviará como `X-Correlation-ID` y conservará el valor devuelto por el backend. Un timeout fijo inicial de 8 segundos usará `AbortController`; expiración y cancelación externa se distinguirán. Los códigos frontend serán `TIMEOUT`, `NETWORK_ERROR`, `INVALID_RESPONSE` y `REQUEST_ABORTED`, mientras los errores HTTP conservarán el código público del backend. No habrá reintentos automáticos, interceptores ni logs de cuerpos.

`credentials: "include"` se conserva para cookies futuras, pero no se implementan cookies ni sesión. El cliente aceptará respuestas exitosas sin contenido cuando una API futura lo requiera, y validará que las respuestas que declaren JSON realmente puedan interpretarse.

### OpenAPI generado y tipos versionados

El backend añadirá `springdoc-openapi-starter-webmvc-api` 3.0.3, compatible con Spring Boot 4, sin Swagger UI. El documento estará en `/v3/api-docs`, se limitará a `/api/v1/**` y estará habilitado en `local` y `test`; la configuración productiva seguirá diferida. El contrato describirá el endpoint, DTOs, `ApiError`, estados, códigos HTTP y header de correlación.

El frontend añadirá únicamente `openapi-typescript` como dependencia de desarrollo. Un script Node pequeño y transversal obtendrá el documento desde `${BACKEND_URL}/v3/api-docs`, guardará una copia canónica en `frontend/openapi/backend-api.json` y generará `frontend/lib/api/generated.ts`. `npm run api:sync` actualizará ambos archivos; `npm run api:check` generará en memoria o temporalmente y fallará si difieren de los archivos versionados. El build y los tests unitarios usarán los tipos confirmados y no necesitarán un backend activo.

Se prefiere generación de tipos sobre escribir DTOs manualmente o generar un SDK: evita duplicación sin introducir capas de transporte, métodos automáticos ni comportamiento oculto.

### Comprobación visible temporal

La página base incorporará un componente cliente pequeño que consulte una sola vez `/api/v1/system/status` al montarse y ofrezca un botón de reintento manual. Mostrará estados `Comprobando`, `Disponible` o `No disponible` con texto y no solo color. En error mostrará un mensaje genérico y, cuando exista, el identificador de correlación; nunca la excepción interna.

No habrá polling, caché global ni almacenamiento. El frontend seguirá renderizando aunque el backend esté detenido, lo que conserva la independencia de la base frontend.

### Pruebas por capas y recorrido real separado

Las pruebas rápidas existentes seguirán sin infraestructura. Se ampliarán con casos unitarios de correlación enviada, timeout, cancelación, respuesta vacía y componente temporal mediante `fetch` simulado.

El backend añadirá pruebas con MySQL Testcontainers para el endpoint `UP`, fallo `503`, CORS permitido/rechazado y presencia del contrato en OpenAPI. Una suite frontend separada, ejecutada mediante `npm run test:integration`, usará el cliente real contra un backend local conectado al MySQL Compose y comprobará datos, header de correlación y tipos generados. La documentación indicará el orden de arranque y no mezclará esta suite con `npm test`.

## Risks / Trade-offs

- **Springdoc introduce divergencia con Spring Boot 4.1** → fijar 3.0.3, ejecutar compilación y pruebas de generación; si la versión no resulta compatible, actualizar a la última 3.0.x verificada antes de continuar.
- **`openapi-typescript` 7.13 exige TypeScript 5.x** → mantener el frontend en TypeScript 5.9.3, versión estable compatible con Next.js 16 y con el generador, en lugar de forzar una combinación de peers incompatible con TypeScript 6.
- **El contrato generado cambia por contenido no determinista** → canonicalizar JSON y excluir metadatos dinámicos antes de comparar.
- **La página genera una consulta MySQL por visita** → hacer una sola comprobación al montar y solo repetir por acción explícita; el endpoint no es monitoreo continuo.
- **CORS con credenciales puede ampliar exposición** → lista exacta de orígenes, métodos y headers; nunca combinar credenciales con comodines.
- **`NEXT_PUBLIC_BACKEND_URL` queda incorporada al bundle** → documentar que es una dirección pública, no un secreto, y exigir reconstrucción al cambiarla.
- **La suite real depende de procesos locales** → mantenerla separada, con precondiciones y errores claros, mientras CI/base unitarios continúan aislados.
- **El proceso del backend puede iniciar aunque MySQL caiga después** → el endpoint ejecuta la comprobación en cada solicitud y convierte el fallo actual en `503`.

## Migration Plan

1. Implementar endpoint, error 503, CORS y OpenAPI en backend con sus pruebas aisladas y Testcontainers.
2. Ampliar variables y cliente frontend; añadir generación de contrato y tipos.
3. Incorporar el indicador temporal y pruebas unitarias.
4. Iniciar MySQL en `3307`, backend en `8080` y frontend en `3000`; sincronizar el contrato y ejecutar la suite real.
5. Documentar comandos, variables, contrato y criterios de regeneración.

La reversión elimina el endpoint técnico, CORS, springdoc, tipos generados y componente temporal. No existe migración de base de datos ni dato que revertir.

## Open Questions

No quedan decisiones abiertas para la base técnica. Los contratos de consulta de certificados, autenticación, revocación y sesión permanecen deliberadamente sin definir.
