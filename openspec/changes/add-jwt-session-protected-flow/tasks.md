## 1. Persistencia y configuración de sesión

- [x] 1.1 Crear la migración Flyway incremental de `cancellation_flow_session` con relación única a solicitud, estado, familia y hashes de refresh, ventana de concurrencia, expiración, invalidación, índices, restricciones y comentarios en español.
- [x] 1.2 Implementar entidad, enums y repositorio JPA con las consultas y bloqueos estrictamente necesarios para crear, validar, rotar e invalidar una sesión.
- [x] 1.3 Añadir configuración externa validada para firma, emisor, audiencia, nombres de cookies, vigencias, ventana de concurrencia y atributos seguros, con valores locales/test no productivos y sin secretos en Git.
- [x] 1.4 Actualizar las pruebas de Flyway desde base limpia y desde V6, validación Hibernate, restricciones uno-a-uno y documentación del modelo.

## 2. Núcleo JWT y ciclo de vida

- [x] 2.1 Implementar emisión y validación estricta de access y refresh JWT con claims técnicos mínimos, algoritmo fijo y ausencia verificable de DNI o datos funcionales.
- [x] 2.2 Implementar cookies separadas `HttpOnly`, rutas y expiraciones correctas, `SameSite=Lax` y `Secure` obligatorio salvo configuración local explícita.
- [x] 2.3 Implementar el servicio transaccional para crear la sesión, resolver su contexto actual y validar en base de datos cada access token.
- [x] 2.4 Implementar renovación bajo bloqueo, rotación de refresh, ventana acotada para carreras entre pestañas y detección de reutilización fuera de ventana.
- [x] 2.5 Implementar logout idempotente con invalidación de familia, limpieza de cookies y abandono exclusivo de solicitudes reversibles.
- [x] 2.6 Cubrir firma, claims, expiración, manipulación, invalidación, rotación, replay, concurrencia, logout y ausencia de tokens/PII en logs mediante pruebas unitarias y de MySQL.

## 3. Integración con inicio e ID Perú existente

- [x] 3.1 Adaptar el inicio público para crear sesión y emitir cookies únicamente después de persistir `AVAILABLE`, manteniendo sin sesión todos los demás resultados.
- [x] 3.2 Reemplazar la validación `IDENTITY_INIT` del inicio de ID Perú por la sesión activa pendiente de identidad, sin aceptar request ID o configuración del proveedor desde frontend.
- [x] 3.3 Adaptar el callback exitoso para actualizar la sesión existente junto con solicitud e intento verificado, conservando state, PKCE, intercambio y validación real/simulada actuales.
- [x] 3.4 Mantener fallos, cancelación y mismatch en step 1 sin elevar la sesión ni emitir una autorización paralela.
- [x] 3.5 Retirar `FlowTokenPurpose`, emisión `FLOW_AUTH`, logout local anterior y campos de autorización general en `identity_verification` después de migrar todas sus referencias.
- [x] 3.6 Añadir pruebas integrales que demuestren una única sesión desde disponibilidad positiva hasta callback real simulado y que ID Perú no se ejecuta fuera de ella.

## 4. Seguridad HTTP y contratos backend

- [x] 4.1 Incorporar una cadena Spring Security simple con principal de sesión, allowlist pública explícita y protección de todas las APIs internas.
- [x] 4.2 Añadir validación exacta de `Origin` para mutaciones autenticadas, manteniendo CORS con credenciales y excluyendo correctamente el callback protegido por state.
- [x] 4.3 Implementar endpoints versionados de sesión actual, refresh y logout con errores uniformes, correlación y limpieza de cookies cuando corresponda.
- [x] 4.4 Implementar autorización del paso según estado persistido y un error estable cuando se intente acceder a una operación futura o incompatible.
- [x] 4.5 Actualizar OpenAPI con esquemas cookie de access/refresh, seguridad por operación, respuestas y DTO seguros; regenerar snapshot y tipos TypeScript.
- [x] 4.6 Probar con MockMvc la matriz pública/protegida, CORS/Origin, cookies, expiración, renovación, logout y bloqueo de pasos futuros.

## 5. Rutas y experiencia frontend

- [x] 5.1 Mantener `/` como home pública server-rendered y redirigir a `/cancelacion` cuando el backend confirme una sesión activa o renovable.
- [x] 5.2 Proteger server-side `/cancelacion` y `/autorizacion` reenviando cookies al backend y redirigir a `/` cuando la sesión sea inválida, sin confiar solo en presencia de cookies.
- [x] 5.3 Mover la consulta inicial que aún viva en el orquestador interno a la home y hacer que el éxito navegue directamente al step 1 protegido sin alerta positiva intermedia.
- [x] 5.4 Implementar un coordinador específico de sesión para estado y una única renovación controlada, conservando el cliente HTTP general sin interceptores ni reintentos globales.
- [x] 5.5 Crear el layout interno común con header institucional, perfil con DNI completo limitado a la sesión autenticada, acción accesible de cerrar sesión y stepper controlado por el paso autorizado.
- [x] 5.6 Implementar cierre desde el header, estado de procesamiento, limpieza mediante backend y retorno a home en todas las pestañas sin usar almacenamiento persistente.
- [x] 5.7 Manejar recarga, foco y múltiples pestañas consultando nuevamente la fuente backend, con mensajes seguros para sesión terminada y backend no disponible.

## 6. Pruebas frontend e integración

- [x] 6.1 Añadir pruebas de renderizado y navegación para home pública, redirección con sesión, guardas internas y bloqueo de acceso directo.
- [x] 6.2 Probar layout, perfil con DNI completo, logout accesible, estados de sesión y ausencia de tokens en props, HTML, URLs y almacenamiento del navegador.
- [x] 6.3 Probar renovación única, carrera recuperable entre pestañas y convergencia del paso permitido después de recarga o cambio en otra pestaña.
- [x] 6.4 Ejecutar una prueba de integración frontend-backend-MySQL desde consulta positiva, sesión, ID Perú mock, recarga interna y logout/abandono.
- [x] 6.5 Verificar que resultados negativos de disponibilidad no creen sesión y que trámites abandonados, vencidos o terminales no sean recuperados.

## 7. Documentación y validación final

- [x] 7.1 Actualizar `PROJECT_CONTEXT.md`, decisiones técnicas, modelo de datos y READMEs para reflejar sesión JWT activa sin recuperación histórica.
- [x] 7.2 Documentar variables, atributos de cookies, claves por ambiente, flujo de renovación, rutas públicas/internas y procedimiento local de prueba sin incluir secretos.
- [x] 7.3 Ejecutar backend `mvn verify`, pruebas frontend, typecheck, build, contrato OpenAPI, validación estricta OpenSpec y revisión de logs/secretos.
- [x] 7.4 Realizar comprobación manual en navegador de home, sesión, ID Perú real local, callback, recarga, dos pestañas, expiración y cierre antes de archivar el cambio.
