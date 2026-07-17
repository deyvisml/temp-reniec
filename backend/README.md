# Backend de cancelación de certificados digitales

Backend técnico construido con Java 21, Spring Boot 4.1.0, Maven y MySQL. Incluye el modelo persistente de solicitudes de cancelación, pero todavía no implementa endpoints ni casos de uso del flujo ciudadano.

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

El perfil `local` importa opcionalmente `.env` desde el directorio de trabajo. Las variables definidas directamente en el proceso tienen mayor precedencia, por lo que pueden reemplazar cualquier valor del archivo. Si no deseas usar `.env`, proporciona al menos `DB_USERNAME` y `DB_PASSWORD` mediante el entorno del proceso.

No confirmes `.env` ni credenciales reales en el repositorio y no uses variables públicas del frontend para secretos. La configuración productiva permanece fuera de este entorno local.

## Compilar y probar

Desde `/backend`, en Windows:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

- `test` ejecuta siete pruebas técnicas rápidas sin MySQL ni Docker.
- `clean verify` ejecuta además las pruebas `*IT`. Testcontainers inicia MySQL 8.4.0, Flyway construye una base vacía, Hibernate valida el esquema y el contenedor se elimina al finalizar.
- `clean` evita conservar recursos compilados obsoletos en `target` después de sustituir una migración.
- El artefacto queda en `target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar`.

En Linux o macOS, usa `./mvnw`.

Después de compilar también puedes ejecutar el backend desde `/backend`:

```powershell
java -jar target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Flyway es el único propietario del esquema y usa `src/main/resources/db/migration/V1__create_cancellation_request_model.sql`. Hibernate aplica `ddl-auto=validate`: no crea ni modifica tablas. Si MySQL no está disponible o una migración no coincide, el backend no inicia.

## Perfiles

- `local`: importa opcionalmente `.env`, conecta mediante `DB_*` y usa logs de aplicación más detallados.
- `test`: puerto aleatorio; las pruebas rápidas excluyen persistencia y las `*IT` reciben MySQL efímero mediante Testcontainers.

La configuración de producción permanece diferida.

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

SPEC-04R reemplazó la V1 provisional de dos tablas. Una base local que ya ejecutó `V1__create_cancellation_persistence.sql` tendrá un checksum incompatible y debe recrearse desde vacío.

Si el volumen local solo contiene datos desechables:

1. Detén el backend.
2. Confirma que no existe información relevante.
3. Ejecuta `docker compose down -v`.
4. Ejecuta `docker compose up -d --wait`.
5. Inicia el backend con `local` para que Flyway aplique la nueva V1.

Nunca limpies, repares o recrees automáticamente una base compartida o con datos relevantes. En ese caso detén el cambio y diseña migraciones hacia adelante.

## Modelo y seguridad

El esquema contiene siete conceptos: solicitud, elegibilidad, identidad, sesiones, revocación, constancias y auditoría. La documentación completa y el diagrama ER están en [`docs/data-model/README.md`](../docs/data-model/README.md).

La persistencia recibe DNI y descripciones ya cifrados, HMAC de búsqueda y hashes de sesión. No almacena DNI completo, texto sensible, tokens, biometría, payloads externos completos ni PDF. No deben registrarse esos valores, sus referencias, últimos cuatro dígitos, credenciales, cuerpos, headers sensibles ni query strings con datos personales.

## Fuera de alcance

Permanecen diferidos endpoints funcionales, JWT, recuperación multidispositivo completa, consulta real de certificados, ID Perú, revocación externa, generación de PDF, criptografía institucional, retención definitiva, módulos administrativos y despliegue productivo. Compose contiene solo MySQL: backend y frontend se ejecutan fuera de contenedores y no se añaden Dockerfiles ni scripts de inicio. El frontend no se modifica en este cambio.
