## Context

El sistema ya crea una solicitud después de DNI, reCAPTCHA y disponibilidad positiva. Para enlazarla con ID Perú emite actualmente JWT temporales con propósitos `IDENTITY_INIT` y `FLOW_AUTH`, guarda la autorización posterior dentro de `identity_verification` y usa una cookie `HttpOnly`. Este mecanismo cubre solo la frontera de autenticación: no modela una sesión renovable, no protege uniformemente los pasos futuros y asigna al intento de ID Perú una responsabilidad que pertenece al trámite completo.

La home debe quedar pública, mientras que el paso 1 y los posteriores pertenecen a una única operación activa. La solución debe admitir recarga y pestañas del mismo navegador, pero no debe recuperar trámites históricos o finalizados, guardar tokens en APIs de almacenamiento del navegador ni reimplementar OAuth/OIDC de ID Perú.

## Goals / Non-Goals

**Goals:**

- Crear una sola sesión transaccional persistida por solicitud activa después de disponibilidad positiva.
- Transportar access y refresh JWT exclusivamente en cookies seguras `HttpOnly`.
- Autorizar cada API y página interna contra el JWT y el estado persistido actual.
- Rotar el refresh token de forma segura y tolerar carreras legítimas entre pestañas sin abrir recuperación multidispositivo.
- Integrar el ID Perú existente dentro de la sesión y elevarla tras la coincidencia de identidad.
- Resolver desde backend el paso permitido y proporcionar el DNI completo exclusivamente mediante el contrato autenticado del header, sin incluirlo en JWT ni cookies.
- Cerrar la sesión y abandonar de manera transaccional una solicitud todavía reversible.

**Non-Goals:**

- Implementar listado o selección de certificados, motivo, confirmación, revocación o constancia.
- Crear login permanente, cuentas ciudadanas, historial, recuperación de operaciones finalizadas o administración de sesiones.
- Guardar DNI, certificados o claims personales en JWT.
- Sustituir o rediseñar los adaptadores real y simulado de ID Perú.
- Introducir Redis, sesiones HTTP tradicionales, múltiples familias activas por solicitud o un proveedor OAuth propio.

## Decisions

### 1. Una entidad de sesión mínima vinculada uno a uno con la solicitud activa

Se añadirá `cancellation_flow_session` con identificador técnico, `request_id` único, estado, familia de refresh, hashes de refresh actual y anterior, ventana breve de concurrencia, expiración, último uso, invalidación y timestamps. La solicitud continúa siendo la raíz del dominio; la sesión solo representa la autorización de la operación activa.

Se elige una tabla porque la revocación inmediata, la rotación de refresh y el cierre requieren estado servidor. Mantener estos datos en `identity_verification` seguiría mezclando autenticación externa con sesión del trámite. No se crea una tabla por token ni por pestaña.

### 2. La sesión nace después de `AVAILABLE`, antes de ID Perú

El caso de uso de inicio persistirá solicitud, consulta positiva y sesión en una frontera transaccional coherente; luego emitirá las dos cookies. La sesión empezará en fase `PENDING_IDENTITY_VERIFICATION`. Resultados negativos o técnicos no emitirán cookies ni sesión.

Esto permite que el paso 1 ya sea interno y que ID Perú opere sobre el mismo contexto. La coincidencia de identidad actualizará solicitud y sesión a `IDENTITY_VERIFIED`; no se emitirá una autorización paralela.

### 3. Access y refresh JWT opacos para el frontend

El access token será corto y contendrá únicamente `sid`, `rid`, `jti`, audiencia, emisor, emisión y expiración. El refresh contendrá además familia y versión técnica. Se firmarán con algoritmo y clave configurados externamente; el backend fijará y verificará algoritmo, emisor y audiencia. Ningún token incluirá DNI, nombre, estado funcional o certificados.

Las cookies serán `HttpOnly`, `SameSite=Lax`, con rutas explícitas; `Secure` será obligatorio bajo HTTPS y solo podrá deshabilitarse en perfil local HTTP. El access cookie cubrirá APIs y renderizado interno; el refresh cookie se limitará al endpoint de renovación. No se devolverán tokens en JSON.

### 4. Validación stateful en cada frontera protegida

La firma válida no bastará. El filtro de seguridad obtendrá la sesión por `sid`, comprobará request, estado activo, expiración, invalidación y operación no terminal. La autorización del paso se derivará del estado persistido de la solicitud, no de claims controlados al emitir el JWT.

Spring Security proporcionará la cadena de filtros y reglas de rutas; el servicio de sesión conservará la emisión y validación con la biblioteca JOSE ya utilizada. Los endpoints públicos se limitarán a inicio, salud, documentación de desarrollo y callback de ID Perú. El callback seguirá protegido por `state`, no por la cookie enviada al dominio de ID Perú.

### 5. Renovación rotatoria con manejo acotado de concurrencia

Cada renovación bloqueará la fila, comparará el hash y rotará el refresh. Para pestañas que enviaron simultáneamente el token inmediatamente anterior, la fila conservará su hash durante una ventana breve configurable. Esa carrera devolverá un conflicto recuperable sin invalidar ni limpiar la sesión; el cliente podrá volver a resolver el estado usando las cookies más recientes. Una reutilización fuera de la ventana invalidará la familia.

La renovación será una operación específica del módulo de sesión, no un interceptor general ni un bucle de reintentos. El frontend realizará como máximo una renovación coordinada por carga cuando una consulta de sesión indique access expirado.

### 6. Protección de páginas mediante validación server-side

`/` consultará el estado de sesión desde el servidor: si existe una sesión activa redirigirá a `/cancelacion`; si no, mostrará la home pública. `/cancelacion` y la ruta local de compatibilidad `/autorizacion` validarán la sesión en el servidor antes de renderizar y redirigirán a `/` si no es válida. La mera presencia de una cookie no se considerará validación.

El renderizado server-side reenviará solo el encabezado Cookie al endpoint interno de estado mediante `BACKEND_URL`. En el navegador, el cliente central seguirá usando `credentials: include`. No se duplicará la clave privada ni lógica de validación JWT en Next.js.

### 7. Contexto actual y navegación autorizada

`GET /api/v1/session/current` devolverá estado de sesión, DNI completo para el perfil autenticado, paso permitido y capacidad de continuar, sin exponer esos datos en la URL, JWT ni cookies. El orquestador renderizará únicamente ese paso. Acceder directamente o intentar invocar un endpoint futuro fuera de orden producirá un error estable de paso no permitido.

El layout interno contendrá header institucional, perfil con DNI completo y salida. Varias pestañas observarán el mismo estado persistido después de recargar o consultar de nuevo; ninguna dependerá de `localStorage`, `sessionStorage` o estado React como fuente de verdad.

### 8. Cierre transaccional y abandono seguro

`POST /api/v1/session/logout` bloqueará sesión y solicitud, invalidará la familia y marcará `ABANDONED` solo si la solicitud se encuentra antes de confirmación y no existe operación irreversible o incierta. Siempre expirará ambas cookies. Para estados irreversibles futuros, el cierre invalidará acceso sin alterar el resultado del dominio.

El logout local de ID Perú existente se reemplazará por este caso de uso. No se inventará logout remoto mientras el contrato institucional no lo exija.

### 9. Protección CSRF y CORS

Además de `SameSite=Lax`, las operaciones autenticadas que cambian estado exigirán `Origin` perteneciente a la allowlist exacta ya usada por CORS. El callback de ID Perú queda excluido y continúa validando `state` de un solo uso. No se aceptarán wildcard con credenciales ni métodos de mutación por GET.

### 10. Migración incremental

Flyway añadirá la tabla con comentarios en español, claves, índices y restricciones. La implementación dejará de emitir y validar `IDENTITY_INIT`/`FLOW_AUTH`; los campos de autorización temporal de `identity_verification` se retirarán mediante migración hacia adelante cuando no exista información productiva que conservar. Las filas históricas de solicitudes e intentos permanecerán.

Durante el cambio, contratos OpenAPI y tipos TypeScript se regenerarán juntos. La documentación técnica se actualizará para eliminar afirmaciones vigentes de “sin tabla de sesiones” y “sin refresh tokens”.

## Risks / Trade-offs

- [Cookies entre `localhost:3000` y `localhost:8080` se comportan distinto a dominios HTTPS] → Documentar ambos ambientes, mantener atributos configurables solo donde sea imprescindible y probar CORS/cookies con navegador real.
- [Rotación concurrente puede producir falsos positivos de reutilización] → Ventana mínima para el hash anterior, bloqueo pesimista de la fila y conflicto recuperable sin invalidación dentro de esa ventana.
- [Una consulta server-side por navegación añade latencia] → Endpoint de estado pequeño, indexado y sin integraciones externas; evita duplicar secretos o confiar solo en presencia de cookies.
- [JWT robado permanece usable hasta expirar] → Access corto, verificación persistida en cada llamada, cookies `HttpOnly`/`Secure`, cierre con invalidación inmediata y ausencia de PII.
- [Crear sesión antes de ID Perú incrementa filas abandonadas] → Relación uno a uno, expiración breve y transición explícita a `ABANDONED`; no se crean registros por pestaña o por access token.
- [Cambiar la autorización actual afecta una integración ya probada] → Conservar state, PKCE, callback y adaptadores; sustituir únicamente la fuente de contexto y cubrir el flujo completo con pruebas reales simuladas.

## Migration Plan

1. Añadir la migración y entidad de sesión sin cambiar todavía las rutas públicas.
2. Implementar emisión, cookies, validación, renovación, cierre y pruebas aisladas.
3. Integrar la creación de sesión con el resultado `AVAILABLE` y adaptar ID Perú para consumir/actualizar esa sesión.
4. Incorporar Spring Security y proteger endpoints internos con una allowlist pública explícita.
5. Añadir endpoint de contexto, guardas server-side, layout interno y navegación desde la home.
6. Retirar la autorización temporal anterior y sus campos una vez que todas las referencias migren.
7. Regenerar OpenAPI/tipos, ejecutar migraciones desde limpio y V6, y validar escenarios de navegador.

Rollback: antes de desplegar tráfico real, la migración de código puede revertirse manteniendo la tabla nueva sin uso. Después de emitir sesiones nuevas no se intentará convertirlas al formato anterior; se invalidarán y el ciudadano deberá iniciar una nueva operación.

## Open Questions

- Confirmar antes de producción los nombres definitivos de cookies, dominio institucional y duraciones operativas de access y refresh; los valores iniciales deberán ser conservadores y configurables.
- Confirmar si la infraestructura frontal y backend compartirán sitio registrable, requisito para definir definitivamente `SameSite` y `Domain` sin debilitar la protección.
