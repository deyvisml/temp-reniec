## Context

El repositorio ya contiene un backend Spring Boot con API `/api/v1`, errores uniformes, correlación, CORS, OpenAPI, JPA/Flyway y MySQL; un frontend Next.js con App Router, Tailwind, cliente HTTP y contratos TypeScript generados; y una integración técnica comprobada entre ambas aplicaciones. La portada actual y su indicador de estado son temporales.

El modelo consolidado ya representa la solicitud y sus intentos de elegibilidad mediante `certificate_cancellation_request` y `certificate_eligibility_check`. El DNI se conserva deliberadamente como `CHAR(8)` legible dentro de MySQL, con prohibición de incluirlo en logs, URLs, errores o endpoints técnicos. Los identificadores numéricos internos tampoco deben exponerse públicamente.

`PROJECT_CONTEXT.md` confirma que la consulta ocurre antes de ID Perú, que el ciudadano no ve certificados individuales y que solo un resultado favorable permite continuar. El contrato institucional de consulta aún no existe, por lo que esta entrega necesita un contrato interno estable y un mock determinista reemplazable. `home.png` guía composición, jerarquía y comportamiento visual, pero no agrega reglas funcionales.

## Goals / Non-Goals

**Goals:**

- Entregar el primer recorrido ciudadano completo desde `/` hasta un resultado de elegibilidad persistido.
- Mantener una única regla de DNI —exactamente ocho dígitos ASCII— compartida conceptualmente y probada en ambos extremos.
- Crear o recuperar una solicitud compatible sin duplicar solicitudes activas bajo concurrencia.
- Registrar cada intento externo y mantener coherentes el estado actual de la solicitud y el resultado del intento.
- Separar el caso de uso del contrato externo mediante un puerto y un mock local determinista.
- Ofrecer estados de formulario, carga, resultados y errores accesibles, responsive y fieles a la referencia visual.
- Mantener OpenAPI y los tipos TypeScript como contrato verificable.

**Non-Goals:**

- Implementar ID Perú, JWT, refresh tokens, recuperación multidispositivo o una sesión autenticada.
- Mostrar certificados individuales o inventar atributos y contratos del proveedor institucional.
- Implementar motivo, confirmación, revocación, constancia o módulos administrativos.
- Crear una página provisional de verificación de identidad.
- Incorporar un rate limiter distribuido, CAPTCHA, WAF o política productiva contra abuso.
- Agregar tablas, cifrado especulativo o un framework arquitectónico adicional.

## Decisions

### 1. Un único endpoint inicia o recupera la solicitud y ejecuta la consulta

Se incorporará `POST /api/v1/cancellation-requests` con cuerpo JSON `{ "dni": "########" }`. Un `POST` evita colocar el DNI en la URL y representa la creación o recuperación controlada del trámite. La respuesta funcional contendrá una referencia pública opaca, DNI enmascarado, estado de solicitud, resultado normalizado, indicador `canContinue`, `nextStep` y si la solicitud fue reutilizada; nunca devolverá el DNI completo ni detalles de certificados.

Los resultados `ELIGIBLE`, `NOT_ELIGIBLE` e `INCONCLUSIVE` tendrán respuesta funcional tipada. Indisponibilidad, timeout, conflicto en curso y error técnico utilizarán códigos HTTP y `ApiError` consistentes, conservando el resultado del intento antes de responder. El frontend interpretará por código estable, no por texto.

Alternativas descartadas:

- `GET` con DNI como parámetro: expone información personal en URLs, historial y telemetría.
- Dos endpoints públicos para crear y consultar: permite dejar solicitudes incompletas y complica la experiencia inicial sin beneficio actual.
- Responder siempre `200`: oculta fallos operativos relevantes al transporte y dificulta observabilidad.

### 2. Una referencia pública UUID simple separa la API del identificador interno

La solicitud obtendrá una columna `public_reference CHAR(36)` única, generada por la aplicación y expuesta solo como referencia opaca. Flyway la añadirá y poblará para instalaciones existentes antes de aplicar `NOT NULL` y unicidad. El `BIGINT` seguirá siendo exclusivamente interno.

La referencia podrá preparar una transición a `/verificacion-identidad?request=<public-reference>` sin incluir el DNI. SPEC-06 solo realizará la navegación autorizada y no creará contenido para esa ruta; la siguiente tarea incorporará la ruta y la sesión JWT. La referencia no autentica ni autoriza operaciones sensibles por sí sola.

Alternativas descartadas:

- Exponer el `BIGINT`: contradice la decisión vigente sobre identificadores públicos.
- Reutilizar correlación: el cliente puede enviarla y representa una petición, no una solicitud ciudadana.
- Crear una sesión temporal o cookie propia: anticiparía y condicionaría la próxima implementación JWT.

### 3. El DNI permanece transparente en MySQL y se minimiza fuera de persistencia

Se mantendrá la columna única `dni`; los dígitos visibles se derivarán al construir la respuesta y no se duplicarán en otra columna. El backend no registrará DTOs ni valores de entrada, el frontend limpiará el valor tras obtener una respuesta final y no lo guardará en `localStorage`, `sessionStorage`, cookies ni URLs.

La validación aceptará únicamente `^[0-9]{8}$`, sin normalizaciones implícitas, letras, espacios o caracteres Unicode. El frontend usará teclado numérico y longitud máxima como ayuda, mientras el backend será la autoridad.

Alternativas descartadas:

- Reintroducir hash/cifrado sin infraestructura de claves: contradice el modelo simplificado y no ofrece protección real.
- Persistir últimos dígitos: duplica un valor derivable y puede desincronizarse.

### 4. La creación y deduplicación se serializan por DNI en MySQL

El caso de uso preparará la consulta dentro de una transacción corta. Buscará con bloqueo pesimista la solicitud más reciente en estados incompatibles usando el índice por DNI, estado y fecha; expirará las que correspondan y decidirá entre recuperar, informar una consulta en progreso o crear una nueva. La definición inicial de incompatibles cubrirá `STARTED`, `CHECKING_ELIGIBILITY`, `ELIGIBLE` y `PENDING_IDENTITY_VERIFICATION`.

- Una solicitud `ELIGIBLE` o `PENDING_IDENTITY_VERIFICATION` se recupera sin repetir la consulta.
- Una solicitud `CHECKING_ELIGIBILITY` produce conflicto controlado y no crea otro intento.
- Una solicitud `STARTED` con resultado transitorio puede recibir el siguiente intento.
- Una solicitud expirada se marca `EXPIRED` antes de crear otra.
- Una solicitud terminal histórica no impide una nueva iniciada explícitamente.

El bloqueo, el índice existente, la unicidad `(request_id, attempt_number)` y `@Version` forman una defensa en profundidad sin triggers ni columnas de guarda. Los conflictos de bloqueo o versión se traducirán a un error reintentable y no a un `500` genérico.

### 5. La llamada externa se separa en preparación, ejecución y finalización

Una primera transacción crea o recupera la solicitud, asigna el siguiente intento, registra `SUBMITTED` y deja la solicitud en `CHECKING_ELIGIBILITY`. El puerto `CertificateEligibilityGateway` se invoca fuera de la transacción para no retener bloqueos durante I/O. Una segunda transacción bloquea la solicitud y completa el intento y su estado actual.

El orquestador capturará timeout y errores controlados para finalizar el intento como `FAILED` y devolver la solicitud a un estado reintentable con resultado `UNAVAILABLE`, `INCONCLUSIVE` o `ERROR`. Un intento `SUBMITTED` que supere el umbral configurable se considerará abandonado por fallo técnico antes de habilitar un nuevo intento. Esto evita que una caída entre transacciones bloquee el DNI indefinidamente.

Transiciones principales:

| Resultado | Intento | Solicitud | Continuidad |
| --- | --- | --- | --- |
| `ELIGIBLE` | `COMPLETED` | `PENDING_IDENTITY_VERIFICATION` | Sí |
| `NOT_ELIGIBLE` | `COMPLETED` | `NOT_ELIGIBLE` | No, terminal |
| `INCONCLUSIVE` | `COMPLETED` | `STARTED` | No, reintento seguro |
| `UNAVAILABLE` | `FAILED` | `STARTED` | No, reintento seguro |
| `ERROR` | `FAILED` | `STARTED` | No, reintento seguro |

### 6. El mock es determinista, local y configurable por perfil

El puerto devolverá un resultado interno propio, referencia técnica opcional y código técnico controlado. El adaptador mock estará activo solo en perfiles local/test mientras no exista un adaptador real. No copiará ni anticipará el contrato institucional.

Se documentarán DNI sintéticos reservados para pruebas locales:

| DNI ficticio | Escenario |
| --- | --- |
| `00000001` | Elegible |
| `00000002` | No elegible |
| `00000003` | Servicio no disponible |
| `00000004` | Resultado no concluyente |
| `00000005` | Error técnico controlado |
| `00000006` | Timeout reproducible |

Cualquier otro DNI válido producirá `NOT_ELIGIBLE` en el mock para mantener comportamiento predecible. Los valores son fixtures locales sin asociación con personas ni certificados reales.

### 7. La portada se implementa como formulario cliente acotado dentro del App Router

El layout conservará semántica, salto al contenido, errores globales y metadatos, pero adaptará encabezado, fondo, contenedor y pie a la jerarquía de `home.png`. El formulario será el único componente cliente necesario; el resto permanecerá renderizable en servidor.

El campo tendrá etiqueta visible, `inputMode="numeric"`, ayuda, error asociado mediante `aria-describedby` y foco dirigido al primer error. El envío deshabilitará el botón, anunciará carga con `aria-live`, usará `AbortController` y evitará tanto doble clic como reenvío mientras exista una operación. Los mensajes de resultado incluirán texto e iconografía, nunca solo color.

La portada dejará de mostrar el indicador técnico de integración. La consulta real demostrará la integración; la comprobación técnica seguirá disponible en pruebas y en `/api/v1/system/status`, no como elemento ciudadano.

La referencia se implementará mediante CSS, componentes accesibles e iconos propios simples. `home.png` no se recortará ni se publicará como pantalla rasterizada, y no se inventarán sellos, enlaces o afirmaciones institucionales que el contexto no confirme.

### 8. La protección básica contra abuso se limita a controles justificables ahora

La entrada tendrá tamaño y formato estrictos, el endpoint solo aceptará JSON, las solicitudes activas y consultas en curso se deduplicarán, el cliente bloqueará envíos simultáneos y los timeouts estarán acotados. Estas medidas reducen automatización accidental y amplificación sin crear una solución de seguridad falsa.

No se implementará un limitador en memoria por IP: no funciona consistentemente con múltiples instancias y puede identificar incorrectamente clientes detrás de proxies. Rate limiting distribuido, CAPTCHA y controles perimetrales quedan documentados como pendientes de infraestructura productiva.

### 9. OpenAPI sigue siendo la fuente de verdad

Los DTO y respuestas se anotarán para que `/v3/api-docs` describa el endpoint, los resultados y los errores esperados. `frontend/openapi/backend-api.json` y `frontend/lib/api/generated.ts` se regenerarán mediante los comandos existentes; `contracts.ts` expondrá únicamente alias y rutas estables. La verificación de drift seguirá fallando cuando backend y frontend no coincidan.

## Risks / Trade-offs

- **[La referencia pública puede confundirse con autorización]** → Documentar y probar que es solo localizador opaco; ninguna operación sensible dependerá de ella sin la futura sesión autenticada.
- **[Una caída tras registrar `SUBMITTED` deja un intento en curso]** → Detectar intentos obsoletos por tiempo antes de reintentar, cerrarlos con error técnico y conservar trazabilidad.
- **[El bloqueo por DNI depende del aislamiento e índice de MySQL]** → Probar concurrencia real con Testcontainers y mantener la consulta bloqueante alineada con `idx_request_dni_status_created`.
- **[El mock puede interpretarse como contrato real]** → Nombrarlo y documentarlo como adaptador local, mantener DTO externos fuera del dominio y evitar payloads inventados.
- **[La respuesta previa a autenticación revela elegibilidad]** → Comunicar solo categorías generales, sin certificados ni detalles técnicos, y someter el texto definitivo a validación institucional.
- **[La fidelidad visual está limitada por ausencia de activos oficiales separados]** → Reproducir estructura y estilo con recursos propios simples; no extraer imágenes del screenshot ni falsificar activos institucionales.
- **[La transición llega a una ruta cuya pantalla pertenece a la siguiente tarea]** → Centralizar `nextStep` y la ruta, probar que solo se emiten con elegibilidad y no crear una pantalla provisional.

## Migration Plan

1. Añadir con Flyway la referencia pública nullable, poblarla para filas existentes, aplicar unicidad y convertirla en obligatoria.
2. Actualizar entidad y repositorio, conservando el resto del esquema y datos.
3. Incorporar el caso de uso, el puerto y el mock bajo perfiles local/test.
4. Exponer el endpoint, actualizar OpenAPI y regenerar contratos TypeScript.
5. Sustituir la portada temporal y retirar únicamente el indicador visual, conservando el endpoint técnico.
6. Ejecutar pruebas backend, frontend, contrato, MySQL y flujo integrado.

Rollback: revertir aplicación y frontend a la versión previa. La columna adicional puede permanecer sin afectar el binario anterior; no se eliminará automáticamente durante rollback para evitar pérdida de referencias. Una eliminación posterior requeriría una migración explícita.

## Open Questions

- Los textos institucionales definitivos, enlaces de ayuda y activos oficiales separados de la imagen siguen pendientes de aprobación.
- El criterio y contrato real del servicio de certificados continúa pendiente; el puerto y el resultado normalizado son internos y reemplazables.
- La política productiva de rate limiting, retención y expiración se definirá cuando exista infraestructura y validación institucional.
- La siguiente tarea deberá decidir cómo la referencia pública se vincula a la sesión JWT y a la verificación de identidad; no debe tratarla como prueba de identidad.
