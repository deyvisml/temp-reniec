# Backend de revocación de credenciales digitales

Backend construido con Java 21, Spring Boot 4.1.0, Maven y MySQL. Incluye persistencia, la API técnica y el flujo ciudadano para consultar y revocar credenciales digitales mediante el proveedor configurado.

## Requisitos previos

- JDK 21 con `JAVA_HOME` configurado o `java` disponible en `PATH`.
- Docker Desktop u otro runtime con Docker Compose para MySQL local y Testcontainers para `mvn verify`.
- Acceso a Maven Central durante la primera ejecución del wrapper.

No es necesario instalar Maven ni MySQL globalmente. El wrapper descarga Maven 3.9.16 y Compose ejecuta MySQL 8.4.

## Inicio rápido local

Ejecuta todos los comandos de esta sección desde `/backend`. La preparación del archivo local se hace una sola vez:

```powershell
Copy-Item .env.example .env
```

Los valores de `.env.example` son credenciales conocidas y exclusivas de desarrollo local. No las reutilices en ambientes compartidos o productivos. `backend/.env` está ignorado por Git.

Por defecto, Compose publica MySQL en `localhost:3308` y lo conecta con el puerto estándar `3306` dentro del contenedor. Así puede convivir con las instalaciones locales que utilicen `localhost:3306` o `localhost:3307`.

Si ya habías creado `.env` con otro puerto, ese archivo ignorado no se actualiza automáticamente al cambiar `.env.example`. Detén Compose con `docker compose down` y cambia únicamente esa línea a `DB_PORT=3308`; no elimines el volumen.

Valida la configuración y levanta únicamente MySQL 8.4:

```powershell
docker compose --env-file .env.example config
docker compose up -d --wait
docker compose ps
```

La columna `PORTS` debe mostrar un mapeo equivalente a `0.0.0.0:3308->3306/tcp`.

Si `3308` también está ocupado, elige otro puerto libre —por ejemplo `3309`— en tu `.env` privado:

```properties
DB_PORT=3309
```

Después ejecuta `docker compose config` y `docker compose ps` para verificar el mapeo. Compose y Spring Boot leen el mismo `DB_PORT`, por lo que no debes modificar `compose.yaml` ni `application-local.yml`.

Para revisar el arranque de MySQL:

```powershell
docker compose logs mysql
```

Compose crea la base, el usuario local y un volumen persistente. No es necesario ejecutar SQL manualmente.

Inicia el backend:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Flyway aplica o valida automáticamente las migraciones antes de que la aplicación quede operativa. Comprueba la salud en otra terminal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

El estado es `UP` cuando la aplicación y MySQL están disponibles. Solo se expone `health` y no muestra detalles internos.

La comprobación consumible por el frontend ejecuta `SELECT 1` contra MySQL y devuelve un contrato saneado:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/system/status -Headers @{ "X-Correlation-ID" = "local-check" }
```

La documentación está habilitada para desarrollo local:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Swagger UI permite explorar y ejecutar las operaciones disponibles contra el backend local. El contrato incluye el estado técnico, el inicio ciudadano, la sesión transaccional y los endpoints de verificación de identidad. Las operaciones internas documentan la cookie de access `FlowSessionCookie`; el refresh permanece exclusivamente en su endpoint de rotación.

## Variables de entorno

| Variable | Predeterminado | Propósito |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Sin perfil | Usa `local` al ejecutar manualmente; las pruebas activan `test`. |
| `SERVER_PORT` | `8080` | Puerto HTTP. |
| `APP_NAME` | `revocacion-credenciales-backend` | Nombre de aplicación. |
| `LOG_LEVEL_ROOT` | `INFO` (`WARN` en test) | Log general. |
| `LOG_LEVEL_APP` | `INFO` (`DEBUG` local) | Log institucional. |
| `DB_HOST` | `localhost` | Host MySQL. |
| `DB_PORT` | `3308` | Puerto MySQL publicado en el host; se mapea al `3306` interno del contenedor. |
| `DB_NAME` | `revocacion_credenciales` | Base MySQL. |
| `DB_USERNAME` | Obligatoria | Usuario MySQL; disponible en la plantilla local. |
| `DB_PASSWORD` | Obligatoria | Contraseña MySQL; disponible en la plantilla local. |
| `MYSQL_ROOT_PASSWORD` | Solo Compose | Contraseña root para inicialización y healthcheck local. |
| `CORS_ALLOWED_ORIGINS` | Vacía (`http://localhost:3000` en local/test) | Lista separada por comas de orígenes frontend exactos. |
| `AVAILABILITY_STALE_ATTEMPT_THRESHOLD` | `30s` | Umbral para cerrar una consulta de existencia interrumpida. |
| `AVAILABILITY_TIMEOUT` | `15s` | Presupuesto máximo de la consulta inicial de disponibilidad. |
| `AVAILABILITY_MOCK_SIMULATED_TIMEOUT` | `2s` | Demora reproducible del fixture de timeout local. |
| `RECAPTCHA_MODE` | `disabled` en local y producción | Desactiva la verificación anti-bot sin bloquear el inicio ciudadano. |
| `ID_PERU_MODE` | `mock` en local; `real` obligatorio en producción | Local permite alternar entre el simulador y el servicio v1. Producción conserva exclusivamente el servicio v2 real. |
| `APP_FRONTEND_BASE_URL` | `http://localhost:3000` en local | Base del frontend; el retorno se deriva como `/revocacion`. |
| `APP_BACKEND_BASE_URL` | `http://localhost:8080` en local | Base del backend; en local se deriva el callback registrado `/api/v1/idperu/callback`. |
| Versión de ID Perú | `v1` en local y `v2` en producción | Se define por perfil: v1 usa `idaas.reniec.gob.pe`; v2 usa `idaas2.reniec.gob.pe` y PKCE. No requiere una variable manual. |
| `ID_PERU_CLIENT_ID` / `ID_PERU_CLIENT_SECRET` | Obligatorias en modo `real` | Credenciales autorizadas exclusivas del backend. |
| `ID_PERU_REFERER` | `/autorizacion` local por defecto; obligatoria en producción | Referer autorizado por RENIEC; HTTP solo se admite para localhost local. |
| `ID_PERU_FLOW_SECRET` | Valor local de desarrollo; obligatorio externo en `prod` | Base64 de exactamente 32 bytes; deriva claves separadas para PKCE y continuidad. |
| `CREDENTIAL_PROVIDER_MODE` | `real` en local y producción | Usa la réplica local en `8081`; `mock` permanece disponible para pruebas aisladas. |
| `CREDENTIAL_PROVIDER_BASE_URL` | `http://localhost:8081` en local | Base común de los tres endpoints oficiales. Producción exige HTTPS. |
| `CREDENTIAL_PROVIDER_API_KEY` | Clave ficticia local | Header privado `x-api-key`; producción exige un secreto externo. |
| `CREDENTIAL_PROVIDER_CONNECT_TIMEOUT` | `3s` | Tiempo máximo para conectar con el proveedor. |
| `CREDENTIAL_PROVIDER_READ_TIMEOUT` | `10s` | Tiempo máximo para leer una respuesta del proveedor. |
| `SESSION_SIGNING_SECRET` | Valor conocido solo en local; obligatorio externo en `prod` | Base64 de al menos 32 bytes para firmar access y refresh JWT. |
| `SESSION_ACCESS_TTL` | `15m` | Vigencia corta del access JWT, alineada con el proyecto de autorización de referencia. |
| `SESSION_REFRESH_TTL` | `3d` | Ventana actualizable de la operación activa; cada rotación válida emite un refresh con esta vigencia. |
| `SESSION_CONCURRENT_REFRESH_WINDOW` | `5s` | Ventana para reconocer una carrera legítima entre pestañas. |

El perfil `local` importa opcionalmente `.env` desde el directorio de trabajo. Las variables definidas directamente en el proceso tienen mayor precedencia, por lo que pueden reemplazar cualquier valor del archivo. Si no deseas usar `.env`, proporciona al menos `DB_USERNAME` y `DB_PASSWORD` mediante el entorno del proceso.

El adaptador Google se conserva únicamente para pruebas técnicas aisladas. Los perfiles `local` y `prod` no necesitan site key, secret, URI ni allowlist de reCAPTCHA.

No confirmes `.env` ni credenciales reales en el repositorio y no uses variables públicas del frontend para secretos. La configuración productiva permanece fuera de este entorno local.

## Compilar y probar

Desde `/backend`, en Windows:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

- `test` ejecuta las pruebas técnicas rápidas sin MySQL ni Docker.
- `clean verify` ejecuta además las pruebas `*IT`. Testcontainers inicia MySQL 8.4.0, Flyway construye una base vacía, Hibernate valida el esquema y el contenedor se elimina al finalizar.
- `clean` evita conservar recursos compilados obsoletos en `target` después de sustituir una migración.
- El artefacto queda en `target/revocacion-credenciales-backend-0.0.1-SNAPSHOT.jar`.

En Linux o macOS, usa `./mvnw`.

Después de compilar también puedes ejecutar el backend desde `/backend`:

```powershell
java -jar target/revocacion-credenciales-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Flyway es el único propietario del esquema y aplica el historial V1–V7 hasta obtener el modelo vigente de ocho tablas. Hibernate aplica `ddl-auto=validate`: no crea ni modifica tablas. Si MySQL no está disponible o una migración no coincide, el backend no inicia.

## Perfiles

- `local`: importa opcionalmente `.env`, conecta mediante `DB_*`, deshabilita reCAPTCHA, consume la réplica real de credenciales en `8081` y usa ID Perú simulado por defecto.
- `test`: puerto aleatorio y adaptador anti-bot determinista sin red; habilita OpenAPI para verificación automatizada, mantiene Swagger UI deshabilitada y entrega MySQL efímero mediante Testcontainers a las `*IT`.
- `prod`: deshabilita reCAPTCHA y exige ID Perú y proveedor de credenciales reales.
- Sin perfil: OpenAPI y Swagger UI permanecen deshabilitados.

La configuración de producción permanece diferida.

## API técnica, CORS y correlación

- `GET /api/v1/system/status` responde `200` con `status`, `database` y `timestamp` cuando backend y MySQL están disponibles.
- Una falla de MySQL responde `503` con el `ApiError` común y código `DEPENDENCY_UNAVAILABLE`, sin detalles JDBC o SQL.
- Toda respuesta incluye `X-Correlation-ID`; un valor cliente válido se conserva y uno ausente o inválido se reemplaza.
- CORS se aplica solo a `/api/**`, permite exactamente los orígenes configurados, `GET`, `POST`, `OPTIONS`, los headers mínimos y credenciales futuras. Nunca usa comodines.
- OpenAPI JSON y YAML se publican en `/v3/api-docs` y `/v3/api-docs.yaml` bajo `local`; el perfil `test` habilita el contrato para pruebas automatizadas.
- Swagger UI se publica en `/swagger-ui.html` únicamente con el perfil `local`. Su exposición productiva permanece pendiente de una decisión posterior.

## Regla de documentación para endpoints

Todo endpoint nuevo o modificado debe actualizar, dentro del mismo incremento:

- Finalidad, etiqueta, parámetros, cabeceras y cuerpo de la operación.
- DTO, campos obligatorios, formatos y validaciones reales.
- Respuestas exitosas, errores controlados y códigos HTTP posibles.
- Propagación de `X-Correlation-ID`.
- Reglas de seguridad únicamente cuando hayan sido implementadas.
- Pruebas de cobertura OpenAPI, snapshot y tipos TypeScript derivados.

Un endpoint no se considera terminado mientras la documentación generada difiera de su comportamiento HTTP. No edites manualmente los contratos generados del frontend. Swagger declara la cookie de access realmente implementada y nunca expone el contenido de access o refresh JWT.

## Verificación de identidad con ID Perú

Tras un resultado `AVAILABLE`, el backend crea una única sesión transaccional y emite access y refresh JWT en cookies `HttpOnly`, `SameSite=Lax` y de corta vigencia. El frontend muestra la verificación dentro de la URL canónica `/revocacion`, sin DNI, `requestId` ni nombre de paso en la URL. El inicio genera `state` y PKCE S256; el callback admite GET de ID Perú v1 y POST compatible, consume el state una sola vez, intercambia el código, valida firma RS256, `kid`, issuer, audience y vigencia, consulta `/userinfo`, compara el DNI en backend y retorna a la ruta configurada para el ambiente.

El retorno siempre responde `303 See Other` hacia una URI frontend fija. Éxitos y fallos no exponen un documento JSON en el navegador ni propagan `code`, `state`, `session_state`, tokens o DNI en la redirección. Solo una identidad verificada recibe la autorización temporal; los resultados controlados regresan al paso 1 mediante un aviso efímero.

Los códigos y tokens de ID Perú no se devuelven al frontend ni se persisten. El verifier se cifra temporalmente con AES-GCM y se elimina al terminar. Una verificación correcta eleva la misma sesión a identidad verificada. Los refresh JWT rotan y solo sus hashes se guardan; logout invalida la familia, elimina ambas cookies y abandona una solicitud reversible. No existe recuperación multidispositivo ni reapertura de trámites finalizados.

El perfil `local` usa ID Perú simulado por defecto y permite seleccionar el servicio real con `ID_PERU_MODE=real`. El mock exitoso devuelve el mismo DNI válido que inició el flujo y el nombre sintético `PRUEBA`. El perfil `prod` exige siempre ID Perú real y `test` conserva escenarios simulados controlados.

### Probar ID Perú real en local

Completa en `backend/.env` exclusivamente las credenciales autorizadas para desarrollo:

```env
ID_PERU_MODE=real
ID_PERU_CLIENT_ID=valor-autorizado
ID_PERU_CLIENT_SECRET=valor-autorizado
APP_FRONTEND_BASE_URL=http://localhost:3000
APP_BACKEND_BASE_URL=http://localhost:8080
```

En el perfil `local`, `ID_PERU_REFERER` ya tiene como valor predeterminado `http://localhost:3000/autorizacion`; solo debe declararse si las credenciales autorizan un origen distinto. El perfil local usa ID Perú v1 y producción selecciona automáticamente v2. El callback usa uniformemente la ruta `/api/v1/idperu/callback`: en local resulta en `http://localhost:8080/api/v1/idperu/callback` y en producción se combina con la base HTTPS productiva. El paso 1 y su retorno local permanecen en `http://localhost:3000/autorizacion`; producción presenta el paso en `/revocacion`. Después de cambiar `ID_PERU_MODE` o las credenciales con Docker, ejecuta `docker compose restart backend`; no es necesario reconstruir la imagen.

Para la integración real local, ejecuta el backend con **JDK 21** (también es la versión de compilación del proyecto). Se comprobó que el endpoint institucional de ID Perú valida su cadena TLS con el almacén de credenciales de JDK 21, mientras que la instalación local de JDK 22 puede rechazarla con `PKIX path building failed`. En IntelliJ selecciona `C:\Program Files\Java\jdk-21.0.11` como JRE de la configuración de ejecución. No se debe desactivar la validación TLS ni agregar un trust manager permisivo para sortear este error.

Tras iniciar una consulta con cualquier DNI válido que no sea un fixture especial, abre `/revocacion`. El resultado positivo debe llevarte a `/autorizacion`; desde allí inicia la verificación. El navegador debe salir hacia la URL institucional, ID Perú debe retornar al callback local y la vista final debe permanecer en `/autorizacion`. Si el DNI autenticado no coincide con el ingresado, el rechazo es esperado y no debe deshabilitarse.

La aplicación mantiene internamente las decisiones estables de ID Perú: conexión 3 s, lectura 5 s, `state` 5 min, caché JWKS 15 min y ACR `face_mobile`. La continuidad del trámite ya no depende de una cookie temporal de ID Perú: utiliza las cookies JWT de sesión documentadas en [`docs/session/README.md`](../docs/session/README.md). Modificar estos valores requiere una necesidad operativa comprobada y un cambio de código revisado.

Migración desde la configuración anterior:

- Las URI individuales de autorización, token, userinfo, JWKS e issuer se derivan de la raíz institucional fija en `application.yml`.
- Las URI de callback y retorno se reemplazan por `APP_BACKEND_BASE_URL` y `APP_FRONTEND_BASE_URL`.
- Las claves separadas de PKCE y firma se reemplazan por `ID_PERU_FLOW_SECRET`; el backend deriva claves distintas por propósito.
- Modo, ACR, timeouts, vigencias, nombre/seguridad de cookie, `max_age` y logout dejan de ser configuración externa.

La referencia obligatoria es [`docs/integrations/id-peru/IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf`](../docs/integrations/id-peru/IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf). Las credenciales del proyecto de referencia no están autorizadas aquí.

## Inicio ciudadano y mock de disponibilidad

`POST /api/v1/revocation-requests` recibe el DNI de ocho dígitos, crea una solicitud y determina si existe al menos una credencial disponible. La evidencia anti-bot es opcional en el contrato y no se envía desde local o producción mientras reCAPTCHA esté deshabilitado. La respuesta incluye `requestId`, DNI enmascarado, estado, `availabilityResult` y siguiente paso autorizado, sin exponer credenciales individuales, cantidad, índice, fecha o UUID.

Los errores anti-bot permanecen definidos exclusivamente para las pruebas del adaptador Google y una eventual reactivación explícita; no forman parte del recorrido local o productivo actual.

El adaptador interno usado por pruebas aisladas es determinista y no representa el contrato institucional:

| DNI ficticio | Resultado |
| --- | --- |
| `00000002` | `NOT_AVAILABLE`: ausencia confirmada; bloquea el avance |
| `00000003` | Servicio no disponible |
| `00000004` | No concluyente |
| `00000005` | Error técnico controlado |
| `00000006` | Timeout |

Cualquier otro DNI válido, incluido `00000001`, devuelve `AVAILABLE` para que el flujo local normal pueda probarse con la misma identidad que autenticará ID Perú. Los fixtures especiales son sintéticos y ningún resultado inicial produce objetos de credencial. Los resultados inconclusos o técnicos nunca se convierten en ausencia confirmada.

## Paso 2: listado y selección local

Después de una identidad verificada, `GET /api/v1/revocation-requests/current/digital-credentials` obtiene una sola vez el listado detallado de credenciales vigentes y revocadas y devuelve en recargas la instantánea persistida. Cada elemento expone `status: ACTIVE | REVOKED` y `revokedAt`; `credentialStatus` determina el estado. Para una credencial revocada se conserva toda fecha interpretable enviada por el proveedor, incluso cuando parezca futura por diferencias de reloj o zona horaria; la fecha es nullable únicamente si está ausente, mal formada o es anterior a la emisión. La selección permanece en memoria durante los pasos 2 y 3 y solo admite credenciales `ACTIVE`.

El perfil `local` usa por defecto `CREDENTIAL_PROVIDER_MODE=real` contra la réplica independiente en `http://localhost:8081`; el backend y el callback de ID Perú permanecen en `http://localhost:8080`. Los tres servicios oficiales comparten `CREDENTIAL_PROVIDER_BASE_URL`, `CREDENTIAL_PROVIDER_API_KEY`, `CREDENTIAL_PROVIDER_CONNECT_TIMEOUT` y `CREDENTIAL_PROVIDER_READ_TIMEOUT`. En local se admite HTTP solo contra loopback; producción obliga HTTPS y credenciales externas completas. El mock interno se conserva para pruebas unitarias y ofrece estos escenarios deterministas:

| DNI ficticio | Listado detallado |
| --- | --- |
| `00000020` | Lista vacía |
| `00000021` | Una credencial |
| `00000022` | Dos credenciales vigentes y una revocada |
| `00000023` | Dos credenciales válidas con UUID repetido e índices distintos |
| `00000024` | UUID inválido, respuesta rechazada |
| `00000025` | Timeout |
| `00000026` | Servicio no disponible |
| `00000027` | Respuesta malformada |
| `00000029` | Índice repetido, respuesta rechazada |
| Cualquier otro DNI válido | Dos credenciales vigentes y una revocada |

La lista vacía o sin credenciales vigentes finaliza en `NO_DIGITAL_CREDENTIALS_AVAILABLE`; en el segundo caso conserva la fotografía de credenciales revocadas. Los errores técnicos restauran el estado reintentable sin dejar una consulta bloqueada. Un UUID puede repetirse con índices distintos; la identidad de cada credencial es la tupla `digitalCredentialUuid + statusListIndex`, y el índice continúa siendo único por solicitud.

## Paso 4: revisión y confirmación

`POST /api/v1/revocation-requests/current/review` valida el borrador sin persistirlo y devuelve el resumen con DNI enmascarado, credencial seleccionada, motivo, consecuencias y versión de consentimiento. `GET` recupera el resumen de una solicitud ya confirmada. Ninguna respuesta ciudadana muestra el UUID.

`POST /api/v1/revocation-requests/current/confirmation` recibe obligatoriamente el UUID y el índice internos, motivo, descripción opcional, aceptación expresa y versión mostrada. El backend resuelve esa tupla contra la fotografía persistida de la solicitud; el UUID, índice y DNI enviados al proveedor proceden de la base de datos. Una confirmación repetida de la misma tupla reutiliza la misma operación idempotente.

La versión inicial es `REVOCACION_CREDENCIALES_DIGITALES_V1`. La UI invoca la confirmación una sola vez, protege dobles envíos y avanza al paso 5 únicamente después de una revocación exitosa con constancia disponible. Tras una respuesta exitosa del proveedor, el backend respeta `REVOCATION_PROPAGATION_DELAY` (60 segundos por defecto), conserva una constancia `PENDING` y la genera mediante un procesador recuperable aunque el navegador se cierre.

En local los PDF se escriben atómicamente y se conservan en `storage/receipts`, fuera del control de versiones. Esta ubicación persiste entre reinicios del backend. Al habilitar `RECEIPT_MODE=filesystem` fuera de local/test, `RECEIPT_STORAGE_ROOT` debe ser una ruta absoluta sobre un volumen persistente con respaldo; MySQL conserva únicamente la referencia segura del archivo.

## Detener y reiniciar MySQL local

La detención normal conserva el volumen y sus datos migrados:

```powershell
docker compose down
```

Para volver a iniciar la misma base:

```powershell
docker compose up -d --wait
```

El siguiente reinicio es destructivo. Úsalo únicamente después de confirmar que el volumen contiene datos locales desechables:

```powershell
docker compose down -v
docker compose up -d --wait
```

Nunca ejecutes el reinicio destructivo contra una base compartida o con información relevante. `flyway clean` permanece deshabilitado.

## Antecedente de la V1 local

La V1 consolidada eliminó estructuras anteriores de sobreingeniería. Desde entonces, V2–V5 evolucionan el esquema exclusivamente hacia adelante; V5 corrige la separación entre disponibilidad inicial y listado autenticado. Una base que ya estaba en V4 se actualiza sin recrearse ni perder credenciales existentes.

Si el volumen local solo contiene datos desechables:

1. Detén el backend.
2. Confirma que no existe información relevante.
3. Ejecuta `docker compose down -v`.
4. Ejecuta `docker compose up -d --wait`.
5. Inicia el backend con `local` para que Flyway aplique la nueva V1.

Nunca limpies, repares o recrees automáticamente una base compartida o con datos relevantes. En ese caso detén el cambio y diseña migraciones hacia adelante.

## Modelo y seguridad

El esquema contiene ocho tablas para solicitud, consulta de disponibilidad, credenciales detallados futuros, identidad, sesión transaccional, revocación, constancias y auditoría. La documentación completa y el diagrama ER están en [`docs/data-model/README.md`](../docs/data-model/README.md).

La solicitud guarda el DNI directamente como ocho dígitos, el estado actual del progreso y el motivo libre como texto limitado para que el esquema sea legible. Estos valores nunca deben aparecer en logs, errores, URLs, endpoints técnicos, cuerpos registrados ni query strings. `requestId` identifica la solicitud pero no autentica al ciudadano. MySQL almacena el estado de la sesión y hashes de refresh; nunca almacena JWT completos, credenciales, biometría, payloads externos completos ni archivos PDF.

## Fuera de alcance

Permanecen diferidos el contrato real del segundo servicio, la revocación externa, la constancia, la recuperación de trámites, la retención definitiva, el rate limiting productivo, los módulos administrativos y el despliegue productivo. Compose contiene solo MySQL.
