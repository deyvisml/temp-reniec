## 1. Alinear la configuración local

- [x] 1.1 Cambiar `DB_PORT` de `3306` a `3307` en `backend/.env.example`, manteniendo intactos los demás valores exclusivamente locales.
- [x] 1.2 Cambiar el fallback de `DB_PORT` del datasource en `application-local.yml` a `3307` y confirmar que el resto de la URL JDBC no cambia.
- [x] 1.3 Validar que `compose.yaml` conserva `${DB_PORT}:3306`, sin fijar ni cambiar el puerto interno del contenedor.

## 2. Documentar el aislamiento de puertos

- [x] 2.1 Actualizar `backend/README.md` para indicar que Compose usa `localhost:3307` y el contenedor usa `3306`, evitando el puerto convencional del MySQL nativo.
- [x] 2.2 Documentar cómo escoger otro `DB_PORT` en el `.env` ignorado si `3307` está ocupado y cómo verificar el mapeo con `docker compose ps`.
- [x] 2.3 Advertir que un `.env` existente no se actualiza automáticamente y debe cambiarse o regenerarse sin eliminar el volumen persistente.

## 3. Validar Compose y Spring Boot

- [x] 3.1 Ejecutar `docker compose --env-file .env.example config` y comprobar que el mapeo predeterminado resuelve el host `3307` hacia el contenedor `3306`.
- [x] 3.2 Comprobar con un archivo de entorno temporal que otro `DB_PORT` libre también se propaga a Compose sin modificar YAML versionado.
- [x] 3.3 Iniciar MySQL en `3307`, confirmar el estado saludable y verificar que no se publica el puerto host `3306`.
- [x] 3.4 Iniciar Spring Boot con `local`, confirmar que Flyway migra o valida mediante `localhost:3307` y que `/actuator/health` responde `UP`.
- [x] 3.5 Ejecutar las pruebas backend pertinentes para confirmar que los perfiles de prueba y Testcontainers permanecen independientes del nuevo puerto local.

## 4. Limpieza y orden OpenSpec

- [x] 4.1 Detener backend y Compose sin eliminar el volumen, retirar archivos temporales y confirmar que no queda ningún `.env` privado en el diff.
- [x] 4.2 Validar estrictamente el cambio y conservar documentado que `add-compose-local-mysql-environment` debe archivarse antes de `change-local-mysql-host-port`.
