## Context

El repositorio contiene el contexto funcional, las referencias UI y las decisiones técnicas iniciales, pero no contiene todavía `/backend`. Esta tarea crea la primera aplicación ejecutable sin adelantar ninguna parte del flujo ciudadano. El backend futuro será un monolito modular y deberá crecer por funcionalidades cuando aparezcan casos de uso reales, persistencia, seguridad e integraciones confirmadas.

La base debe ser útil desde el primer commit: compilar, iniciar, responder salud, producir errores consistentes y permitir correlacionar solicitudes. A la vez, debe operar sin MySQL, servicios externos, secretos ni infraestructura adicional.

## Goals / Non-Goals

**Goals:**

- Crear un proyecto Spring Boot único y ejecutable bajo `/backend` con Java 21 y Maven.
- Exponer únicamente la salud técnica mínima y establecer convenciones reutilizables para errores, validación, logs y correlación.
- Separar configuración común, local y de pruebas mediante mecanismos estándar de Spring.
- Proporcionar pruebas rápidas que ejerciten la aplicación por HTTP sin dependencias externas.
- Dejar una estructura pequeña que permita añadir módulos por funcionalidad sin reorganización prematura.

**Non-Goals:**

- Implementar endpoints, reglas, entidades o casos de uso del flujo de cancelación de certificados.
- Añadir JWT, Spring Security, MySQL, JPA, migraciones, auditoría funcional o persistencia.
- Crear contratos, clientes, interfaces o mocks para ID Perú, consulta de certificados, revocación o constancias.
- Crear `/frontend`, Docker, despliegue productivo, módulos administrativos o un proyecto Maven multi-módulo.
- Aplicar arquitectura hexagonal exhaustiva, CQRS, event sourcing, colas, microservicios o capas vacías.

## Decisions

### Versiones y construcción reproducible

Se fijarán Java 21, Spring Boot 4.1.0 y Maven Wrapper 3.9.16. Spring Boot 4.1.0 es una versión estable compatible con Java 21 y Maven 3.6.3 o posterior; Maven 3.9.16 es la versión estable recomendada al definir este cambio. El wrapper permitirá compilar con una versión consistente sin exigir una instalación global de Maven. Se usará un solo `pom.xml`, empaquetado `jar` y el parent de Spring Boot para mantener alineadas las dependencias administradas.

Como alternativa se consideró Spring Boot 3.5.x por su mayor antigüedad, pero no existe compatibilidad heredada que conservar y 4.1.0 es la versión estable vigente. No se usará Maven 4 porque permanece como preview.

Fuentes de versión: [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html), [Spring Boot Build Systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html) y [Apache Maven Downloads](https://maven.apache.org/download.cgi).

### Dependencias mínimas

El `pom.xml` contendrá `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator` y `spring-boot-starter-test` con alcance de prueba. Se elige `spring-boot-starter-webmvc` porque en Spring Boot 4 sustituye al starter `web` deprecado y proporciona Spring MVC con Tomcat embebido. Las pruebas HTTP usarán el cliente HTTP del JDK para evitar otro starter de prueba web.

No se añadirán starters de Security, Data, JDBC, JPA, observabilidad distribuida, documentación OpenAPI ni clientes HTTP externos. Spring Boot administrará las versiones transitivas; no se fijarán versiones individuales.

### Coordenadas, paquete y organización

El proyecto usará `pe.gob.reniec` como `groupId`, `cancelacion-certificados-backend` como `artifactId` y `pe.gob.reniec.certificados.cancelacion` como paquete base. La clase principal permanecerá en la raíz del paquete para que el escaneo cubra futuras funcionalidades.

Solo se crearán componentes con uso actual:

- `shared.error` para el contrato y el manejador global de errores.
- `shared.web` para el filtro de correlación.
- Recursos de configuración y pruebas.

No se creará todavía un paquete vacío de cancelación ni separaciones `domain`, `application`, `infrastructure` o `api`. Cuando exista la primera funcionalidad, se añadirá un paquete de característica que podrá contener esas separaciones si aportan valor real.

### Configuración y perfiles

`application.yml` contendrá valores comunes y placeholders con valores seguros para `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT` y `LOG_LEVEL_APP`. `application-local.yml` contendrá únicamente ajustes de desarrollo local. `application-test.yml` usará puerto aleatorio o el puerto proporcionado por la prueba y niveles de log reducidos. Las pruebas activarán explícitamente el perfil `test`; la ejecución local documentará `SPRING_PROFILES_ACTIVE=local`.

No habrá perfil de producción en esta tarea, secretos, credenciales ni valores sensibles. Spring podrá sobrescribir propiedades mediante variables de entorno usando su configuración externalizada estándar.

### Salud técnica con Actuator

Se expondrá por HTTP únicamente `health` bajo `/actuator/health`, con detalles deshabilitados para clientes. El indicador comprobará que el contexto y el servidor web responden; no se añadirá un indicador personalizado ni una dependencia de base de datos. No se crearán controladores productivos adicionales para ping o errores de demostración.

### Correlación HTTP segura

Un `OncePerRequestFilter` atenderá `X-Correlation-ID`. Un valor recibido será válido si contiene entre 1 y 64 caracteres ASCII y cumple `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`. Si falta, está vacío, excede la longitud o contiene caracteres no permitidos, se sustituirá por un UUID generado por el servidor.

El filtro guardará el valor como atributo de solicitud, lo incorporará al MDC bajo `correlationId`, lo devolverá en el header de todas las respuestas y limpiará el MDC en `finally`. También registrará al finalizar únicamente método HTTP, ruta sin query string, estado y correlación; no registrará headers, cuerpos ni parámetros.

Se consideró aceptar cualquier header o usar trazabilidad distribuida, pero la validación evita inyección en logs y una plataforma de tracing sería prematura.

### Contrato y manejo global de errores

Una respuesta JSON de error contendrá exactamente los campos base `code`, `message`, `timestamp`, `path` y `correlationId`. `timestamp` será UTC en formato ISO-8601. Un `@RestControllerAdvice` traducirá al menos errores de validación y solicitudes mal formadas a 400, rutas no encontradas a 404 cuando Spring las enrute al advice, métodos no soportados a 405 y excepciones no controladas a 500.

Los mensajes públicos serán estables y comprensibles; no incluirán nombres de clases, mensajes internos, stack traces, datos sensibles ni detalles de implementación. El handler recuperará la correlación del atributo establecido por el filtro. El escenario de prueba del error se implementará mediante un controlador definido solo en fuentes de prueba, por lo que producción conservará únicamente Actuator.

### Validación sin reglas de dominio

El starter de Validation habilitará Jakarta Bean Validation para futuros DTO. La prueba controlada usará un DTO exclusivo de pruebas con una restricción estándar para verificar la integración y el formato de error. No se implementarán validaciones de DNI, motivos ni ninguna regla funcional.

### Pruebas de integración acotadas

Las pruebas usarán `@SpringBootTest` con servidor real en puerto aleatorio y perfil `test`. Cubrirán: carga del contexto, respuesta `UP` de `/actuator/health`, error de validación con el contrato común, generación de correlación cuando no se envía, propagación exacta de un header válido y sustitución de un header inválido. No usarán MySQL, contenedores, red externa ni servicios simulados.

### Documentación y política de logs

`backend/README.md` incluirá requisitos previos, comandos del wrapper para compilar, ejecutar y probar, URL de salud y variables disponibles. También declarará la política mínima de no registrar DNI completos, tokens, credenciales, biometría ni datos personales innecesarios. El patrón de logs incluirá `[correlationId]` sin agregar dependencias de logging.

## Risks / Trade-offs

- [Spring Boot 4 es una generación reciente] → Fijar 4.1.0, usar únicamente APIs estables y ejecutar la suite completa con Java 21 durante la implementación.
- [El handler global podría capturar demasiado y ocultar información útil] → Mantener códigos públicos simples, logs internos mínimos y pruebas por categoría; ampliar el contrato solo cuando existan necesidades de API reales.
- [Un identificador suministrado por el cliente podría contaminar logs] → Aplicar longitud y allowlist estrictas y generar un UUID ante cualquier incumplimiento.
- [El log por solicitud puede generar ruido, especialmente en salud] → Registrar solo metadatos mínimos y permitir ajustar el nivel mediante `LOG_LEVEL_APP`.
- [Los perfiles iniciales podrían interpretarse como configuración productiva] → Documentar que `local` y `test` son los únicos perfiles cubiertos y posponer producción.
- [Crear paquetes compartidos demasiado pronto puede convertirse en un cajón genérico] → Limitar `shared` a error y web, ambos reutilizados desde esta tarea, y crear futuros componentes dentro de sus funcionalidades.

## Migration Plan

1. Generar `/backend` como proyecto Maven único con wrapper, Java 21 y las dependencias mínimas.
2. Añadir configuración común, `local` y `test`, manteniendo toda propiedad externalizable y sin secretos.
3. Implementar correlación, logs y manejo de errores con sus contratos mínimos.
4. Configurar Actuator para exponer solo salud y añadir la documentación local.
5. Añadir las pruebas de contexto, salud, errores y correlación.
6. Ejecutar `mvnw verify`, iniciar con perfil local y comprobar `/actuator/health` antes de completar las tareas.

La incorporación no requiere migración de datos ni despliegue. Si fuera necesario revertirla, puede retirarse `/backend` porque todavía no existen consumidores, datos ni integraciones dependientes.

## Open Questions

Ninguna para la base técnica. Puertos, configuración productiva, JWT, MySQL, contratos externos y requisitos funcionales se definirán en cambios posteriores.
