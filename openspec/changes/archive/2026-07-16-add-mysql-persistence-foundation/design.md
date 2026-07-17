## Context

El backend es un monolito Spring Boot 4.1.0 con Java 21, Maven, Web MVC, Validation y Actuator. Sus perfiles `local` y `test`, el formato de errores y la correlación ya existen, pero actualmente no hay datasource, persistencia ni módulos funcionales. El frontend es un paquete Next.js independiente y no consume aún endpoints funcionales.

El contexto funcional exige persistir el progreso en MySQL y permitir una recuperación futura después de volver a verificar la identidad. También exige minimizar la exposición del DNI y evita definir contratos de ID Perú, consulta o revocación antes de contar con información oficial. Por ello este cambio crea únicamente la estructura durable necesaria para procesos y sus sesiones, sin un API o caso de uso ciudadano.

## Goals / Non-Goals

**Goals:**

- Configurar MySQL, JPA y Flyway con valores externos y perfiles compatibles con la base existente.
- Crear desde una base vacía un esquema pequeño de dos tablas y validarlo al iniciar.
- Representar un proceso completo mediante una única entidad persistente, con vigencia, estado, actividad, timestamps y bloqueo optimista.
- Conservar solo una referencia técnica irreversible y datos parciales del DNI; nunca el DNI completo en texto plano.
- Permitir varias sesiones por proceso, cada una con referencia irreversible, expiración e invalidación.
- Probar el comportamiento contra MySQL real efímero sin requerir una instalación manual.
- Mantener los health checks y logs sin detalles sensibles.

**Non-Goals:**

- Implementar endpoints, casos de uso, validación funcional del DNI o reglas completas de transición.
- Implementar JWT, refresh tokens, cookies, recuperación de progreso o recuperación multidispositivo.
- Guardar motivo, consentimiento, resultado de revocación, constancia, datos de ID Perú o respuestas externas.
- Diseñar auditoría funcional, cifrado institucional definitivo, retención productiva o alta disponibilidad.
- Añadir Docker Compose, una segunda base de datos, caché, procedimientos almacenados, triggers o módulos administrativos.
- Modificar `/frontend` o los diseños de referencia.

## Decisions

### 1. Dependencias gestionadas por Spring Boot

Se añadirán `spring-boot-starter-data-jpa`, `flyway-core`, el módulo MySQL de Flyway y `mysql-connector-j`; para pruebas se usarán el soporte Testcontainers de Spring Boot y el módulo MySQL de Testcontainers. Se conservará el proyecto Maven de un solo módulo y no se fijarán versiones que ya gestione Spring Boot 4.1.0.

Alternativa descartada: JDBC directo o una herramienta ORM adicional. Spring Data JPA cubre las operaciones requeridas y evita dos mecanismos de persistencia.

### 2. MySQL es obligatorio en ejecución normal y efímero en integración

La URL se construirá con `DB_HOST`, `DB_PORT` y `DB_NAME`; `DB_USERNAME` y `DB_PASSWORD` serán obligatorios para el perfil local y no tendrán credenciales predeterminadas. `application-test.yml` mantendrá valores no sensibles y las pruebas de persistencia inyectarán dinámicamente la conexión de un `MySQLContainer` mediante `@ServiceConnection` o el mecanismo equivalente soportado.

Las pruebas web técnicas existentes conservarán un contexto rápido y explícitamente sin auto-configuración de datasource/JPA/Flyway. Las pruebas que validan la aplicación completa y la persistencia usarán el contenedor. Así una prueba unitaria o web aislada no necesita Docker, mientras que ninguna prueba de persistencia usa H2 ni una instalación manual de MySQL.

Las clases de integración se nombrarán `*IT` y Maven Failsafe las ejecutará en `verify`; Surefire conservará `mvn test` para la base rápida. Esta separación no crea módulos ni perfiles de producto adicionales.

Alternativas descartadas: H2, porque puede ocultar diferencias de dialecto; una MySQL compartida, porque hace las pruebas dependientes del equipo; y Docker Compose, porque no es necesario para este cambio.

### 3. Flyway es la única autoridad del esquema

Una migración inicial `V1__create_cancellation_persistence.sql` creará ambas tablas, claves foráneas, restricciones e índices. JPA usará `ddl-auto=validate`, `open-in-view=false` y timestamps UTC. Flyway validará las migraciones al inicio y tendrá `clean` deshabilitado. No habrá datos semilla ni credenciales en SQL.

Alternativa descartada: `ddl-auto=create/update`, porque produce cambios no versionados y no garantiza reproducción desde una base vacía.

### 4. Una tabla principal concentra el proceso

`cancellation_process` contendrá:

- `id` como UUID interno almacenado eficientemente y no usado todavía en URLs.
- `dni_reference_hash` como valor opaco de 64 caracteres hexadecimales preparado para una derivación con secreto institucional.
- `dni_last_four` para una presentación futura parcialmente oculta.
- `status` como cadena controlada por un enum Java.
- `active`, `created_at`, `updated_at`, `expires_at` y `version`.

El resultado mínimo de elegibilidad se expresa con `ELIGIBLE` o `NOT_ELIGIBLE` dentro de `status`; no se crea una columna duplicada. Los estados iniciales serán `STARTED`, `ELIGIBLE`, `NOT_ELIGIBLE`, `PENDING_IDENTITY_VERIFICATION`, `EXPIRED` y `ABANDONED`. No habrá tabla catálogo ni restricción SQL que enumere estados, de modo que nuevos valores puedan añadirse sin cambiar la estructura. La entidad mantendrá coherencia entre estados terminales y `active`, pero no implementará una máquina completa de transiciones.

Alternativas descartadas: tablas por paso/estado, un modelo genérico clave-valor o JSON con campos futuros. Todas dificultan integridad y consultas sin aportar valor actual.

### 5. El DNI completo no entra en la persistencia

Esta base no calculará hashes a partir de un DNI ni aceptará el DNI como identificador de entidad. El campo `dni_reference_hash` recibirá únicamente una referencia ya derivada y validará su formato. Un SHA-256 directo del DNI no es suficiente debido a su bajo espacio de búsqueda; una tarea posterior deberá integrar HMAC u otra pseudonimización institucional con gestión segura de claves. Si fuera imprescindible recuperar el DNI, se añadirá mediante una migración posterior un valor cifrado y versionado cuando exista la infraestructura criptográfica.

No se reserva ahora una columna de ciphertext vacía, porque sería un campo especulativo. `dni_last_four` no permite reconstruir el DNI y deberá tratarse igualmente como dato personal en logs y respuestas.

### 6. La tabla de sesiones está justificada por cardinalidad y ciclo de vida

`cancellation_session` tendrá su propio UUID, FK al proceso, `session_reference_hash` único, `created_at`, `expires_at` e `invalidated_at`. Varias filas podrán referir al mismo proceso, que es la condición necesaria para una recuperación futura desde más de un navegador. La tabla no almacenará JWT, refresh tokens, cookies ni secretos reversibles.

La FK no borrará sesiones en cascada: los procesos no se eliminarán físicamente en este alcance y una eliminación futura deberá responder a una política de retención explícita.

Alternativa descartada: columnas de sesión dentro del proceso, porque impedirían representar de forma normalizada más de una sesión y mezclarían ciclos de vida diferentes.

### 7. Repositorios concretos, sin capa de servicio vacía

Se crearán un repositorio para procesos y uno para sesiones. El primero usará `save`/`findById` de Spring Data y una consulta derivada que encuentre el proceso activo no expirado más reciente por `dni_reference_hash`. El segundo permitirá guardar y localizar una sesión por su referencia irreversible. No se expondrán repositorios genéricos propios, ports, adaptadores, controladores ni servicios sin comportamiento actual.

Las actualizaciones de estado cargarán la entidad y usarán un método explícito que actualice estado y actividad. No se usará un `UPDATE` masivo que eluda `@Version`.

### 8. Integridad, tiempo y concurrencia

Los timestamps se mapearán a `Instant` y se completarán mediante auditoría JPA o callbacks equivalentes con una fuente UTC. La base exigirá valores no nulos, expiración posterior a creación, referencias con longitudes definidas, FK válida y unicidad de la referencia de sesión. Se crearán solo índices para la FK de sesión, la búsqueda de proceso por referencia/actividad/expiración y la referencia única de sesión.

`@Version` se mapeará a `version`; dos transacciones que actualicen la misma versión provocarán `OptimisticLockingFailureException` o su excepción JPA equivalente. No se añadirán bloqueos pesimistas ni coordinación distribuida.

### 9. Health agregado y fallos de conexión seguros

Actuator seguirá exponiendo solo `/actuator/health` y `show-details=never`. Con la aplicación completa, el indicador estándar de datasource participará en el estado agregado: una conexión disponible produce `UP` y una pérdida posterior produce estado no saludable sin URL, usuario, SQL ni credenciales. Si MySQL no está disponible durante el arranque, Flyway impedirá iniciar con un error operativo en logs; no se creará un endpoint ni un error API especial porque el servidor aún no está disponible.

### 10. Alcance de las pruebas

Una clase base reutilizable iniciará un único contenedor MySQL por suite o contexto. Las pruebas verificarán: migración desde esquema vacío y validación JPA; creación/lectura; cambio de estado y timestamps; consulta de proceso vigente frente a expirado/inactivo; dos sesiones para un proceso e invalidación; FK/NOT NULL/unicidad; conflicto optimista; y arranque de la aplicación completa. Los datos serán sintéticos, sin DNI reales, y no se hará ninguna llamada externa.

## Risks / Trade-offs

- [Un hash simple de DNI sería enumerable] → El modelo acepta solo una referencia opaca; se documenta que la derivación productiva debe ser HMAC o un mecanismo institucional y queda fuera de esta tarea.
- [El campo `active` duplica parcialmente información del estado] → La entidad será el único punto normal de actualización y las pruebas comprobarán la coherencia; el campo se conserva porque simplifica búsquedas vigentes y fue solicitado explícitamente.
- [Testcontainers requiere un runtime de contenedores] → Se documentará como requisito solo para integración, se reutilizará el contenedor y no se exigirá MySQL manual.
- [Flyway puede impedir el arranque por deriva o indisponibilidad] → Es intencional para evitar operar con un esquema desconocido; logs y health no expondrán secretos.
- [Una referencia de sesión puede confundirse con una sesión autenticada] → Los nombres y documentación aclararán que es infraestructura persistente y no demuestra autenticación ni implementa JWT.
- [Sin política de retención no se pueden borrar datos] → No se automatizará eliminación; expiración e invalidación permiten excluir registros sin destruirlos.

## Migration Plan

1. Añadir dependencias y configuración externa sin credenciales.
2. Incorporar la migración V1 y validar su ejecución sobre un contenedor MySQL vacío.
3. Añadir entidades y repositorios y comprobar que `ddl-auto=validate` coincide con Flyway.
4. Adaptar pruebas web aisladas y añadir la suite de integración completa.
5. Crear en local la base y usuario con privilegios mínimos, configurar variables y arrancar el backend.

La migración es aditiva y no transforma datos existentes. En ambientes compartidos no se usará `flyway clean` ni se borrarán tablas automáticamente. Antes de contener datos, un rollback puede retirar la aplicación y usar una migración compensatoria revisada; después de almacenar datos, cualquier rollback deberá preservar las tablas y ser aprobado como cambio separado.

## Open Questions

- Duración predeterminada de procesos y sesiones; las pruebas usarán instantes explícitos y no fijarán todavía una regla funcional.
- Mecanismo institucional y rotación de claves para derivar `dni_reference_hash`.
- Periodo de retención y eliminación de procesos y sesiones expirados.
- Versión operativa exacta de MySQL 8.4 LTS que aprobará infraestructura para producción.
