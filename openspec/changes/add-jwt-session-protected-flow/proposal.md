## Why

La consulta pública y la autenticación con ID Perú ya permiten iniciar el trámite, pero la continuidad actual depende de una autorización temporal específica del intento de identidad y no ofrece una sesión transaccional renovable que proteja uniformemente todo el flujo. Es necesario consolidar ahora una única sesión activa por solicitud para que los pasos internos, las recargas, varias pestañas y el cierre voluntario tengan un comportamiento seguro y predecible antes de implementar nuevas funcionalidades ciudadanas.

## What Changes

- **BREAKING**: se reemplaza la cookie temporal `IDENTITY_INIT`/`FLOW_AUTH` por una sesión transaccional persistida, con access token y refresh token JWT transportados exclusivamente mediante cookies `HttpOnly`.
- La sesión se crea únicamente después de validar DNI, reCAPTCHA y disponibilidad positiva; una respuesta negativa o técnica no crea sesión.
- La página `/` permanece pública y contiene la consulta inicial. `/cancelacion` y las rutas de compatibilidad desde el paso 1 quedan protegidas por la sesión vigente y por el estado autorizado de la solicitud.
- El backend centraliza emisión, validación, renovación, rotación e invalidación de tokens; los JWT contienen solo identificadores técnicos de sesión y solicitud.
- ID Perú conserva su integración real y simulada, pero inicia y finaliza dentro de la misma sesión. Una identidad coincidente eleva el estado de esa sesión sin crear una segunda credencial paralela.
- Se añade resolución del paso permitido desde el backend, incluyendo bloqueo de accesos directos a pasos futuros y redirección segura ante sesión ausente, vencida o revocada.
- Se incorpora un layout interno común con un perfil que muestra el DNI completo únicamente dentro de la sesión autenticada y una acción de salida.
- El cierre de sesión invalida la sesión, elimina ambas cookies y marca como abandonada la solicitud activa cuando todavía puede abandonarse con seguridad.
- La renovación del access token rota el refresh token y controla reutilización, concurrencia entre pestañas y expiración, sin recuperación de solicitudes históricas o finalizadas.
- OpenAPI, tipos TypeScript, documentación técnica y pruebas se actualizan como parte del mismo incremento.

## Capabilities

### New Capabilities

- `citizen-flow-session`: Creación, persistencia, JWT de acceso y renovación, cookies seguras, rotación, invalidación, estado actual y cierre de la única sesión transaccional activa.
- `protected-citizen-flow`: Protección de rutas y APIs internas, resolución del paso permitido, layout autenticado y comportamiento coherente ante recargas y varias pestañas.

### Modified Capabilities

- `temporary-flow-authorization`: Sustituir la autorización efímera ligada al intento de identidad por la sesión única creada después de la consulta positiva.
- `citizen-eligibility-entry`: Crear la sesión al finalizar positivamente la consulta pública y separar la home pública del primer paso interno.
- `id-peru-citizen-authentication`: Ejecutar el paso 1 dentro de la sesión existente y actualizarla tras verificar una identidad coincidente.
- `single-cancellation-flow-route`: Reservar `/` para el inicio público y mantener `/cancelacion` como ruta canónica de los pasos internos, conservando solo la compatibilidad local estrictamente necesaria para el retorno registrado.
- `cancellation-request-persistence-model`: Incorporar la persistencia mínima de la sesión activa y eliminar de `identity_verification` la responsabilidad de representar la autorización general del flujo.
- `frontend-backend-integration`: Documentar y sincronizar contratos de sesión, cookies, renovación, errores de autenticación y cierre.
- `frontend-foundation`: Incorporar protección server-side de páginas internas y un layout común sin almacenar tokens ni datos personales en APIs del navegador.

## Impact

- Backend Spring Boot: inicio de solicitud, seguridad web, servicio de sesión, cookies, JWT, integración ID Perú, manejo de errores, OpenAPI y configuración externa.
- MySQL/Flyway: nueva tabla de sesión y migración de las referencias temporales de autorización existentes sin recuperar operaciones históricas.
- Frontend Next.js: home pública, guardas server-side, orquestador interno, renovación controlada, layout/header interno, cierre y contratos generados.
- Seguridad: claves de firma externas, hashes de refresh token, cookies con atributos por ambiente, protección CSRF compatible con cookies y validación persistida.
- Pruebas: unidades, persistencia, seguridad HTTP, navegación, recarga, múltiples pestañas, integración con ID Perú y contrato OpenAPI.
