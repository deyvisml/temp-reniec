## 1. Configuración local compartida

- [x] 1.1 Crear `backend/.env.example` con host, puerto, base, usuario y contraseñas exclusivamente locales para MySQL 8.4.
- [x] 1.2 Añadir una regla Git acotada que ignore `backend/.env` sin ignorar `backend/.env.example`, y comprobar ambas rutas con `git check-ignore`.
- [x] 1.3 Actualizar `application-local.yml` para importar opcionalmente `.env` como Config Data, conservando la precedencia de las variables del proceso y la configuración existente de datasource.

## 2. MySQL mediante Docker Compose

- [x] 2.1 Crear `backend/compose.yaml` con un único servicio `mysql:8.4`, inicialización mediante variables de `.env`, publicación configurable del puerto y sin contenedores de aplicación.
- [x] 2.2 Añadir un volumen nombrado persistente y un healthcheck de MySQL que no exponga la contraseña en el comando expandido por el host.
- [x] 2.3 Validar la estructura e interpolación con `docker compose --env-file .env.example config` y confirmar que solo se define el servicio MySQL.

## 3. Documentación del flujo local

- [x] 3.1 Reemplazar la preparación manual recomendada de MySQL en `backend/README.md` por el flujo de copia única de `.env.example`, validación e inicio con `docker compose up -d --wait`.
- [x] 3.2 Documentar estado y logs, detención normal con conservación del volumen, y reinicio destructivo mediante `docker compose down -v` con advertencia para datos locales desechables.
- [x] 3.3 Documentar el arranque Maven con el perfil `local`, el comportamiento automático de Flyway, la consulta de `/actuator/health`, la precedencia de configuración y la alternativa de variables del sistema.
- [x] 3.4 Verificar que la documentación no introduce scripts PowerShell, Dockerfiles, contenedores de backend/frontend, credenciales productivas ni instrucciones de despliegue.

## 4. Comprobación integral

- [x] 4.1 Crear temporalmente `backend/.env` desde el ejemplo, iniciar Compose y confirmar que el contenedor alcanza el estado saludable usando los comandos documentados.
- [x] 4.2 Iniciar Spring Boot con el perfil `local`, comprobar que Flyway migra o valida una base vacía y verificar que `GET /actuator/health` responde `UP`.
- [x] 4.3 Ejecutar la verificación Maven existente para confirmar que las pruebas rápidas y las pruebas de persistencia con Testcontainers continúan pasando sin depender del Compose local.
- [x] 4.4 Detener el entorno, retirar cualquier `.env` temporal si corresponde y confirmar que no quedan archivos privados ni cambios funcionales incluidos en el diff.
