# Decisiones técnicas iniciales

Este documento registra la base técnica acordada para futuras etapas. No configura ni implementa todavía ninguna parte del sistema. El contexto funcional completo se encuentra en [`context/PROJECT_CONTEXT.md`](./context/PROJECT_CONTEXT.md).

## Contexto funcional vigente y alineación

- `docs/context/PROJECT_CONTEXT.md` es la única fuente funcional vigente y prevalece sobre documentos técnicos o diseños que todavía describan el flujo anterior.
- La consulta inicial recibe el DNI y devuelve únicamente si existen certificados disponibles; no devuelve ni persiste lista, cantidad o datos individuales.
- Después de autenticar al ciudadano, un segundo servicio obtiene las emisiones vigentes con número de orden, fecha de creación y UUID para su persistencia y selección en el paso 2.
- Un resultado positivo inicial seguido de una lista vacía bloquea el avance y se comunica como ausencia actual de certificados, no como error de autenticación.
- El flujo permite seleccionar uno, varios o todos los certificados disponibles. Después de confirmar, esa selección es inmutable y forma el conjunto atómico de revocación; los no seleccionados permanecen fuera de la operación.
- La integración de revocación deberá enviar la lista completa bajo una única clave de idempotencia y aceptar solo un resultado común: exitoso, fallido o incierto. Un proveedor que produzca resultados mixtos por UUID será incompatible y no se normalizará como resultado parcial.
- `revocation_operation.normalized_result` será la fuente técnica del resultado; no se persistirán filas de resultado por certificado. La constancia identificará el conjunto seleccionado y su resultado común.
- Las adaptaciones de contratos, persistencia y vistas se realizarán mediante cambios funcionales posteriores, sin anticipar contratos institucionales externos aún no confirmados.

## Tecnologías y estructura

- Backend con Spring Boot.
- Frontend con Next.js.
- Base de datos MySQL.
- Estilos con Tailwind CSS.
- Dos carpetas principales futuras en la raíz del repositorio: `/backend` y `/frontend`.

## Sesión y progreso

- Se utilizará JWT para la sesión y la comunicación autenticada entre frontend y backend.
- El progreso del proceso se persistirá en el backend y en MySQL.
- `request_status` controla las transiciones y conserva la trazabilidad de la solicitud actual; no se utilizará para restaurar una solicitud cuando el ciudadano vuelva a la página de inicio.
- Cada nuevo ingreso del DNI desde inicio creará una solicitud y una consulta nuevas. Una solicitud anterior no confirmada podrá quedar `ABANDONED` y las solicitudes terminales permanecerán como historial.
- No se recuperarán automáticamente selecciones, resultados ni constancias anteriores. Una eventual consulta histórica de constancias será un caso de uso independiente y autenticado.
- Una revocación confirmada en curso o con resultado incierto podrá bloquear otro inicio para proteger la idempotencia, sin devolver ni reabrir el trámite anterior.
- Un resultado incierto se reconciliará conservando la misma operación y clave de idempotencia; no habilitará automáticamente otra ejecución.
- JWT se diseñará por separado y no implica una tabla de sesiones, refresh tokens, registros por navegador o dispositivo ni recuperación del progreso.

## Principios de arquitectura y datos

- La arquitectura será sencilla, incremental y fácil de mantener.
- La base de datos se diseñará sin sobreingeniería.
- No se creará una tabla por cada pantalla, paso o estado del flujo.
- El esquema efectivo conserva una tabla de certificados asociados a la solicitud y una operación técnica global; no requiere una tabla de resultados individuales porque la regla es todos o ninguno.
- No se utilizarán microservicios, colas, event sourcing, CQRS ni patrones complejos sin una necesidad comprobada.

## Integraciones y alcance

- Las integraciones externas se definirán mediante interfaces y usarán mocks reemplazables mientras no existan contratos oficiales.
- La integración ID Perú se rige por el PDF aprobado v1.2 conservado en `docs/integrations/id-peru/`; toda modificación de autenticación, PKCE, tokens, datos del ciudadano, JWKS o logout debe revisarlo previamente.
- ID Perú utilizará OAuth 2.0/OpenID Connect Authorization Code con PKCE S256. El backend controlará `state`, PKCE, códigos, tokens, validación criptográfica y comparación del DNI.
- Existirán adaptadores real y simulado seleccionados por configuración. Producción no podrá iniciar con el simulador y el modo real fallará de forma cerrada si falta configuración institucional.
- Después de validar la identidad se emitirá una autorización temporal mediante cookie `HttpOnly`, ligada a la solicitud actual y validada contra `identity_verification`; no habrá tabla de sesiones, refresh tokens ni recuperación de trámites anteriores.
- El flujo ciudadano utiliza `/cancelacion` como única URL canónica. Los pasos se resuelven mediante estado controlado y contexto temporal del backend, sin codificar el paso, DNI, identificador de solicitud, certificados o resultados de autenticación en la URL.
- La consulta pública inicial está protegida por Google reCAPTCHA v2 Checkbox. El frontend conserva el token solo en memoria y el backend lo valida mediante un puerto antes de persistir o consultar disponibilidad.
- La integración Google usa `RestClient`, timeout acotado y allowlist exacta de hostnames. No persiste CAPTCHA, IP ni payloads, y no incorpora reintentos, circuit breaker, fingerprinting o rate limiting en memoria.
- No se inventarán contratos para la consulta de certificados, la revocación o aspectos de ID Perú no definidos en el PDF y el convenio, como parámetros de logout remoto.
- No se incluirán módulos administrativos ni funcionalidades fuera del flujo ciudadano.

Los modelos definitivos, contratos externos y detalles de implementación se decidirán en cambios posteriores cuando exista información validada.
