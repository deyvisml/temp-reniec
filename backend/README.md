# Backend de cancelación de certificados digitales

Backend construido con Java 21, Spring Boot 4.1.0, Maven y MySQL. Incluye persistencia, la API técnica y el primer caso de uso ciudadano para iniciar una solicitud y consultar elegibilidad mediante un mock local reemplazable.

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

Por defecto, Compose publica MySQL en `localhost:3307` y lo conecta con el puerto estándar `3306` dentro del contenedor. Así puede convivir con una instalación nativa de MySQL que utilice `localhost:3306`.

Si ya habías creado `.env` con `DB_PORT=3306`, ese archivo ignorado no se actualiza automáticamente al cambiar `.env.example`. Detén Compose con `docker compose down` y cambia únicamente esa línea a `DB_PORT=3307`; no elimines el volumen.

Valida la configuración y levanta únicamente MySQL 8.4:

```powershell
docker compose --env-file .env.example config
docker compose up -d --wait
docker compose ps
```

La columna `PORTS` debe mostrar un mapeo equivalente a `0.0.0.0:3307->3306/tcp`.

Si `3307` también está ocupado, elige otro puerto libre —por ejemplo `3308`— en tu `.env` privado:

```properties
DB_PORT=3308
```

Después ejecuta `docker compose config` y `docker compose ps` para verificar el mapeo. Compose y Spring Boot leen el mismo `DB_PORT`, por lo que no debes modificar `compose.yaml` ni `application-local.yml`.

Para revisar el arranque de MySQL:

```powershell
docker compose logs mysql
```

Compose crea la base, el usuario local y un volumen persistente. No es necesario ejecutar SQL manualmente.

Inicia el backend:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
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

OpenAPI está habilitado únicamente con los perfiles `local` y `test` en `http://localhost:8080/v3/api-docs`. El documento contiene solo `/api/v1/**`; no incluye Actuator, rutas de prueba ni Swagger UI.

## Variables de entorno

| Variable | Predeterminado | Propósito |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Sin perfil | Usa `local` al ejecutar manualmente; las pruebas activan `test`. |
| `SERVER_PORT` | `8080` | Puerto HTTP. |
| `APP_NAME` | `cancelacion-certificados-backend` | Nombre de aplicación. |
| `LOG_LEVEL_ROOT` | `INFO` (`WARN` en test) | Log general. |
| `LOG_LEVEL_APP` | `INFO` (`DEBUG` local) | Log institucional. |
| `DB_HOST` | `localhost` | Host MySQL. |
| `DB_PORT` | `3307` | Puerto MySQL publicado en el host; se mapea al `3306` interno del contenedor. |
| `DB_NAME` | `cancelacion_certificados` | Base MySQL. |
| `DB_USERNAME` | Obligatoria | Usuario MySQL; disponible en la plantilla local. |
| `DB_PASSWORD` | Obligatoria | Contraseña MySQL; disponible en la plantilla local. |
| `MYSQL_ROOT_PASSWORD` | Solo Compose | Contraseña root para inicialización y healthcheck local. |
| `CORS_ALLOWED_ORIGINS` | Vacía (`http://localhost:3000` en local/test) | Lista separada por comas de orígenes frontend exactos. |
| `ELIGIBILITY_STALE_ATTEMPT_THRESHOLD` | `30s` | Umbral para cerrar una consulta interrumpida. |
| `ELIGIBILITY_TIMEOUT` | `1s` | Tiempo máximo de la integración de elegibilidad. |
| `ELIGIBILITY_MOCK_SIMULATED_TIMEOUT` | `2s` | Demora reproducible del fixture de timeout local. |

El perfil `local` importa opcionalmente `.env` desde el directorio de trabajo. Las variables definidas directamente en el proceso tienen mayor precedencia, por lo que pueden reemplazar cualquier valor del archivo. Si no deseas usar `.env`, proporciona al menos `DB_USERNAME` y `DB_PASSWORD` mediante el entorno del proceso.

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
- El artefacto queda en `target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar`.

En Linux o macOS, usa `./mvnw`.

Después de compilar también puedes ejecutar el backend desde `/backend`:

```powershell
java -jar target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Flyway es el único propietario del esquema y aplica una V1 consolidada de seis tablas. Hibernate aplica `ddl-auto=validate`: no crea ni modifica tablas. Si MySQL no está disponible o una migración no coincide, el backend no inicia.

## Perfiles

- `local`: importa opcionalmente `.env`, conecta mediante `DB_*` y usa logs de aplicación más detallados.
- `test`: puerto aleatorio; las pruebas rápidas excluyen persistencia y las `*IT` reciben MySQL efímero mediante Testcontainers.

La configuración de producción permanece diferida.

## API técnica, CORS y correlación

- `GET /api/v1/system/status` responde `200` con `status`, `database` y `timestamp` cuando backend y MySQL están disponibles.
- Una falla de MySQL responde `503` con el `ApiError` común y código `DEPENDENCY_UNAVAILABLE`, sin detalles JDBC o SQL.
- Toda respuesta incluye `X-Correlation-ID`; un valor cliente válido se conserva y uno ausente o inválido se reemplaza.
- CORS se aplica solo a `/api/**`, permite exactamente los orígenes configurados, `GET`, `POST`, `OPTIONS`, los headers mínimos y credenciales futuras. Nunca usa comodines.
- OpenAPI se publica en `/v3/api-docs` bajo `local` y `test` para sincronizar el contrato del frontend.

## Inicio ciudadano y mock de elegibilidad

`POST /api/v1/cancellation-requests` recibe exclusivamente JSON con un DNI de ocho dígitos. Crea o recupera una solicitud compatible, registra el intento y devuelve `requestId`, DNI enmascarado, estado, resultado y siguiente paso autorizado. El identificador numérico no autentica ni autoriza; el DNI completo no aparece en URLs, errores ni logs.

El adaptador de perfiles `local` y `test` es determinista y no representa el contrato institucional:

| DNI ficticio | Resultado |
| --- | --- |
| `00000001` | Elegible |
| `00000002` | No elegible |
| `00000003` | Servicio no disponible |
| `00000004` | No concluyente |
| `00000005` | Error técnico controlado |
| `00000006` | Timeout |

Cualquier otro DNI válido devuelve no elegible. Son fixtures sintéticos sin relación con ciudadanos reales. Los resultados transitorios permiten reintento sobre la misma solicitud; una consulta en curso devuelve conflicto controlado y una solicitud sin finalizar se recupera sin límite temporal ni repetición innecesaria de la integración.

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

## Sustitución de la V1

La simplificación del modelo reemplaza la V1 anterior por una línea base de seis tablas, sin sesiones persistentes, UUID público, versiones de consentimiento, ventanas de expiración ni control optimista genérico. Una base local que ya ejecutó una versión anterior de V1 tendrá un checksum incompatible y debe recrearse desde vacío si sus datos son desechables.

Si el volumen local solo contiene datos desechables:

1. Detén el backend.
2. Confirma que no existe información relevante.
3. Ejecuta `docker compose down -v`.
4. Ejecuta `docker compose up -d --wait`.
5. Inicia el backend con `local` para que Flyway aplique la nueva V1.

Nunca limpies, repares o recrees automáticamente una base compartida o con datos relevantes. En ese caso detén el cambio y diseña migraciones hacia adelante.

## Modelo y seguridad

El esquema contiene seis conceptos: solicitud, elegibilidad, identidad, revocación, constancias y auditoría. La documentación completa y el diagrama ER están en [`docs/data-model/README.md`](../docs/data-model/README.md).

La solicitud guarda el DNI directamente como ocho dígitos, el estado actual del progreso y el motivo libre como texto limitado para que el esquema sea legible. Estos valores nunca deben aparecer en logs, errores, URLs, endpoints técnicos, cuerpos registrados ni query strings. `requestId` identifica la solicitud pero no autentica al ciudadano. MySQL no almacena sesiones, tokens, credenciales, biometría, payloads externos completos ni archivos PDF.

## Fuera de alcance

Permanecen diferidos JWT, la interfaz completa de recuperación, consulta institucional real, ID Perú, motivo, confirmación, revocación externa, constancia, criptografía institucional, retención definitiva, rate limiting productivo, módulos administrativos y despliegue productivo. Compose contiene solo MySQL.
