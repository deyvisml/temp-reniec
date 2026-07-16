## Why

El repositorio aún no dispone de una aplicación backend ejecutable sobre la cual incorporar de forma incremental los casos de uso del sistema. Crear una base técnica mínima y verificable ahora evita que las siguientes tareas mezclen scaffolding, observabilidad y convenciones HTTP con lógica funcional sensible.

## What Changes

- Crear un único proyecto Maven en `/backend` con Java 21 y Spring Boot 4.1.0.
- Incorporar solamente Spring Web MVC, Spring Validation, Spring Boot Actuator y las dependencias de prueba necesarias.
- Definir el paquete base institucional `pe.gob.reniec.certificados.cancelacion` y una organización inicial orientada a funcionalidades, creando únicamente componentes con utilidad inmediata.
- Configurar perfiles simples `local` y `test`, propiedades externalizables mediante variables de entorno y ausencia de secretos en el repositorio.
- Exponer `/actuator/health` como único endpoint técnico productivo para comprobar que la aplicación inició y responde, sin comprobaciones de MySQL.
- Establecer un formato común de error sin datos sensibles ni trazas internas, junto con manejo global básico para errores controlados, validaciones y fallos inesperados.
- Incorporar correlación HTTP mediante `X-Correlation-ID`: aceptar valores válidos, generar uno cuando falte o sea inválido, añadirlo al MDC de logs y devolverlo en la respuesta.
- Añadir configuración básica de logs que incluya la correlación y documente la prohibición de registrar DNI completos, tokens, credenciales, biometría o datos personales innecesarios.
- Añadir pruebas rápidas e independientes de MySQL y servicios externos para arranque, salud, errores y correlación.
- Documentar brevemente requisitos, compilación, ejecución, pruebas, salud y variables de entorno.
- Mantener fuera del cambio JWT, MySQL, migraciones, modelo de datos, integraciones, mocks, funcionalidades ciudadanas, frontend, administración, Docker y patrones arquitectónicos innecesarios.

## Capabilities

### New Capabilities

- `backend-foundation`: Define la aplicación backend ejecutable, su configuración por ambientes, salud técnica, contrato común de errores, correlación, logs, pruebas mínimas y documentación operativa inicial.

### Modified Capabilities

Ninguna.

## Impact

- Se añadirá el nuevo directorio `/backend` con código Java, configuración Maven, recursos, pruebas y documentación local.
- Se habilitará únicamente la superficie HTTP técnica mínima de Actuator; no se crearán APIs del dominio ciudadano.
- No se introducirán dependencias de base de datos, seguridad, mensajería, integraciones externas ni infraestructura de despliegue.
- Las referencias permanentes en `docs/` permanecen sin modificación y continúan gobernando el alcance funcional y técnico del proyecto.
