## 1. Scaffold del proyecto

- [x] 1.1 Crear `/backend` como proyecto Maven único con Java 21, Spring Boot 4.1.0, Maven Wrapper 3.9.16 y empaquetado JAR
- [x] 1.2 Configurar `pom.xml` únicamente con Web MVC, Validation, Actuator y el soporte mínimo de pruebas administrado por Spring Boot
- [x] 1.3 Crear la clase principal bajo `pe.gob.reniec.certificados.cancelacion` y añadir exclusiones de build apropiadas sin crear paquetes o capas vacías
- [x] 1.4 Comprobar que el proyecto resuelve dependencias y compila con el Maven Wrapper sobre Java 21

## 2. Configuración y salud

- [x] 2.1 Crear `application.yml` con nombre, puerto, exposición exclusiva de `health`, detalles restringidos y variables de entorno con valores seguros
- [x] 2.2 Crear configuraciones acotadas para los perfiles `local` y `test`, sin perfil productivo, secretos ni dependencias externas
- [x] 2.3 Verificar que `/actuator/health` responde `UP` y que otros endpoints de Actuator no quedan expuestos por HTTP

## 3. Correlación y logs

- [x] 3.1 Implementar el filtro `X-Correlation-ID` con validación por allowlist, generación UUID, atributo de solicitud, MDC, header de respuesta y limpieza garantizada
- [x] 3.2 Añadir un log de finalización por solicitud con método, ruta sin query string, estado y correlación, sin headers, cuerpos ni parámetros
- [x] 3.3 Configurar el patrón y niveles básicos de logs mediante variables de entorno sin agregar dependencias adicionales

## 4. Errores y validación

- [x] 4.1 Implementar el modelo JSON común de error con `code`, `message`, `timestamp`, `path` y `correlationId`
- [x] 4.2 Implementar el manejo global de validación, solicitud mal formada, método no soportado, ruta no encontrada y error inesperado con mensajes públicos seguros
- [x] 4.3 Confirmar que Jakarta Bean Validation queda disponible sin añadir validadores de DNI ni reglas del flujo ciudadano

## 5. Pruebas automatizadas

- [x] 5.1 Añadir una prueba de carga del contexto con el perfil `test`
- [x] 5.2 Añadir pruebas HTTP de salud y exposición restringida de Actuator sobre puerto aleatorio
- [x] 5.3 Añadir un controlador y DTO exclusivos de pruebas para verificar el error de validación y la ausencia de detalles internos
- [x] 5.4 Añadir pruebas de generación, propagación y sustitución de identificadores de correlación
- [x] 5.5 Ejecutar `mvnw verify` y confirmar que la suite pasa sin MySQL, contenedores, red externa ni mocks funcionales

## 6. Documentación y revisión final

- [x] 6.1 Crear `backend/README.md` con requisitos, comandos de wrapper, perfiles, variables de entorno y consulta de salud
- [x] 6.2 Documentar la política de no registrar DNI completos, tokens, credenciales, biometría, cuerpos, headers de autorización ni datos personales innecesarios
- [x] 6.3 Iniciar el JAR con el perfil `local`, consultar `/actuator/health` y detener el proceso limpiamente
- [x] 6.4 Revisar árbol, dependencias y rutas finales para confirmar que no existen frontend, JWT, MySQL, Docker, integraciones, módulos administrativos, lógica ciudadana ni arquitectura especulativa
