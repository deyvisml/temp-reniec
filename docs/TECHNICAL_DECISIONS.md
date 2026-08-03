# Decisiones técnicas iniciales

Este documento registra la base técnica acordada para futuras etapas. No configura ni implementa todavía ninguna parte del sistema. El contexto funcional completo se encuentra en [`context/PROJECT_CONTEXT.md`](./context/PROJECT_CONTEXT.md).

## Contexto funcional vigente y alineación

- `docs/context/PROJECT_CONTEXT.md` es la única fuente funcional vigente y prevalece sobre documentos técnicos o diseños que todavía describan el flujo anterior.
- La consulta inicial recibe el DNI y devuelve únicamente si existen credenciales disponibles; no devuelve ni persiste lista, cantidad o datos individuales.
- Después de autenticar al ciudadano, un segundo servicio obtiene todas las credenciales digitales con número de orden, fecha de creación, UUID, estado `ACTIVE | REVOKED` y fecha de revocación cuando corresponde. Solo las vigentes pueden seleccionarse en el paso 2.
- Después de un resultado inicial positivo, el contrato del segundo servicio exige al menos una credencial vigente. Una respuesta vacía o sin vigentes se considera inválida, bloquea el avance y no genera una instantánea parcial.
- El flujo exige seleccionar exactamente una credencial disponible por solicitud. Elegir otro antes de confirmar reemplaza la selección; los demás permanecen fuera de la operación.
- La integración de revocación envía un solo UUID bajo una clave de idempotencia estable y acepta un resultado exitoso, fallido o incierto. No existe resultado parcial dentro de una solicitud.
- `revocation_operation.normalized_result` es la fuente técnica del resultado y la constancia identifica el único credencial seleccionada.
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
- La operación activa utiliza una única sesión transaccional persistida por solicitud. El access JWT y el refresh JWT contienen solo identificadores técnicos y viajan en cookies `HttpOnly`; esto no habilita recuperación histórica ni sesiones por dispositivo.

## Principios de arquitectura y datos

- La arquitectura será sencilla, incremental y fácil de mantener.
- La base de datos se diseñará sin sobreingeniería.
- No se creará una tabla por cada pantalla, paso o estado del flujo.
- El esquema efectivo conserva una tabla de credenciales asociados a la solicitud y una operación técnica global; no requiere una tabla de resultados individuales porque la regla es todos o ninguno.
- No se utilizarán microservicios, colas, event sourcing, CQRS ni patrones complejos sin una necesidad comprobada.

## Integraciones y alcance

- Las integraciones externas se definirán mediante interfaces y usarán mocks reemplazables mientras no existan contratos oficiales.
- El segundo servicio de credenciales utiliza un puerto propio y un mock determinista en desarrollo. Como no existe un contrato institucional verificado, el adaptador real no inventa endpoints ni payloads y el modo `real` falla de forma cerrada hasta incorporarlo mediante un incremento específico.
- La consulta detallada se reserva en una transacción breve, ejecuta la llamada externa fuera de la transacción y persiste atómicamente la colección completa de credenciales vigentes y revocadas. Una recarga reutiliza la instantánea persistida y no vuelve a consumir el proveedor.
- La selección y el motivo permanecen únicamente en memoria durante los pasos 2 y 3. Una recarga descarta ese borrador y vuelve a la selección de credencial; no se usa almacenamiento del navegador.
- El paso 4 valida el borrador mediante una revisión backend de solo lectura. La aceptación expresa guarda exactamente una credencial en `revocation_request_digital_credential`, el motivo, `confirmed_at`, la versión estable `REVOCACION_CREDENCIALES_DIGITALES_V1` y un único evento `CONSENT_CONFIRMED` dentro de una misma transacción.
- La confirmación es el punto de no retorno para modificar selección o motivo. Después de esa transacción corta, el backend ejecuta la revocación fuera de la transacción usando la misma clave de idempotencia y persiste el resultado. Un éxito inicia una espera de propagación autoritativa, calculada desde `revocation_operation.completed_at`; la constancia queda `PENDING` y un procesador backend recuperable la genera al vencer el umbral, independientemente de la pestaña del ciudadano.
- El frontend no simula el minuto mediante una petición bloqueada: recibe las fechas del servidor, presenta el progreso y consulta el estado. Solo `RECEIPT_AVAILABLE` habilita el paso 5.
- El texto completo de consentimiento permanece versionado en el backend; MySQL conserva solo su identificador de versión para evitar duplicación y permitir demostrar qué texto fue aceptado.
- La integración ID Perú se rige por el PDF aprobado v1.2 conservado en `docs/integrations/id-peru/`; toda modificación de autenticación, PKCE, tokens, datos del ciudadano, JWKS o logout debe revisarlo previamente.
- ID Perú utilizará OAuth 2.0/OpenID Connect Authorization Code con PKCE S256. El backend controlará `state`, PKCE, códigos, tokens, validación criptográfica y comparación del DNI.
- Existirán adaptadores real y simulado seleccionados por configuración. Producción no podrá iniciar con el simulador y el modo real fallará de forma cerrada si falta configuración institucional.
- La sesión nace únicamente después de una disponibilidad positiva y acompaña el mismo trámite durante ID Perú. La verificación exitosa eleva esa sesión existente; no crea una autorización paralela. Logout invalida la familia, elimina cookies y abandona solo una solicitud todavía reversible.
- `/` permanece pública. `/revocacion` y la variante local `/autorizacion` son internas y se autorizan con el estado persistido, sin colocar DNI, solicitud, tokens o pasos en la URL.
- El flujo ciudadano utiliza `/revocacion` como única URL canónica. Los pasos se resuelven mediante estado controlado y contexto temporal del backend, sin codificar el paso, DNI, identificador de solicitud, credenciales o resultados de autenticación en la URL.
- La consulta pública inicial no utiliza reCAPTCHA en local ni producción. El contrato conserva evidencia anti-bot opcional únicamente para pruebas técnicas y una posible reactivación futura.
- La integración Google usa `RestClient`, timeout acotado y allowlist exacta de hostnames. No persiste CAPTCHA, IP ni payloads, y no incorpora reintentos, circuit breaker, fingerprinting o rate limiting en memoria.
- No se inventarán contratos para la consulta de credenciales, la revocación o aspectos de ID Perú no definidos en el PDF y el convenio, como parámetros de logout remoto.
- No se incluirán módulos administrativos ni funcionalidades fuera del flujo ciudadano.

Los modelos definitivos, contratos externos y detalles de implementación se decidirán en cambios posteriores cuando exista información validada.
