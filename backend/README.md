# Backend de cancelación de certificados digitales

Base técnica del backend construida con Java 21, Spring Boot 4.1.0 y Maven. En esta etapa no contiene funcionalidades del flujo ciudadano, persistencia, seguridad ni integraciones externas.

## Requisitos previos

- JDK 21 con `JAVA_HOME` configurado o `java` disponible en `PATH`.
- Acceso a Maven Central en la primera ejecución del wrapper para descargar Maven 3.9.16 y las dependencias.

No es necesario instalar Maven globalmente, MySQL, Docker ni servicios externos.

## Compilar y probar

Desde `/backend`, en Windows:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd verify
```

En Linux o macOS, sustituir `.\mvnw.cmd` por `./mvnw`.

`verify` ejecuta las pruebas y genera el JAR ejecutable en `target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar`.

## Ejecutar localmente

Con el plugin de Spring Boot:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Con el JAR empaquetado:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
java -jar target/cancelacion-certificados-backend-0.0.1-SNAPSHOT.jar
```

Consultar la salud técnica:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

La respuesta operativa contiene `{"status":"UP"}`. Ningún otro endpoint de Actuator se expone por HTTP.

## Perfiles

- `local`: desarrollo local; usa el puerto configurado y un nivel de log de aplicación más detallado por defecto.
- `test`: activado por la suite automatizada; usa puerto aleatorio y no depende de infraestructura externa.

La configuración de producción se definirá en una tarea posterior.

## Variables de entorno

| Variable | Valor predeterminado | Propósito |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sin perfil activo | Selecciona `local` al ejecutar manualmente; las pruebas seleccionan `test` |
| `SERVER_PORT` | `8080` | Puerto HTTP del backend |
| `APP_NAME` | `cancelacion-certificados-backend` | Nombre de la aplicación Spring |
| `LOG_LEVEL_ROOT` | `INFO` (`WARN` en pruebas) | Nivel de log general |
| `LOG_LEVEL_APP` | `INFO` (`DEBUG` local, `WARN` en pruebas) | Nivel de log del paquete institucional |

No se deben almacenar secretos ni credenciales en los archivos del repositorio.

## Correlación y política de logs

El backend acepta `X-Correlation-ID` cuando cumple el formato documentado o genera un UUID. El identificador se devuelve en la respuesta y aparece en los logs de la solicitud.

No se deben registrar:

- DNI completos ni otros datos personales innecesarios.
- Tokens o credenciales.
- Datos biométricos.
- Cuerpos de solicitudes o respuestas.
- Headers de autorización u otros headers sensibles.
- Query strings que puedan contener información del ciudadano.

Los logs HTTP de esta base se limitan al método, la ruta sin query string, el estado y el identificador de correlación.
