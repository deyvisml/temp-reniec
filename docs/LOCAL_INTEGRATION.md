# Integración local: MySQL → proveedor local → backend → frontend

El proyecto ofrece dos modalidades locales. Para probar la aplicación completa sin instalar Java, Node.js ni MySQL, desde la raíz ejecuta:

```powershell
docker compose up --build -d --wait
```

Ese stack contiene frontend `3000`, backend `8080` y MySQL `3308`; no contiene la réplica del proveedor. Los pasos siguientes describen la modalidad de desarrollo en la que únicamente las dependencias se ejecutan en Docker y backend/frontend se levantan manualmente.

El stack completo monta obligatoriamente `backend/.env` como archivo privado de solo lectura. Antes de iniciarlo completa allí `ID_PERU_CLIENT_ID` e `ID_PERU_CLIENT_SECRET`; el perfil local usa ID Perú real y no habilita el simulador.

Ambas modalidades son alternativas porque publican los mismos puertos. Ejecuta `docker compose down` en la carpeta de la modalidad activa antes de cambiar, sin agregar `-v`.

## 1. MySQL

Desde `/backend`, crea una sola vez el archivo privado y levanta la base:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env config
docker compose up -d --wait
docker compose ps
```

El resultado debe mostrar `3308->3306` y estado saludable. Compose crea la base y usuario; no ejecutes SQL manual. El volumen `revocacion-credenciales-digitales-local_mysql-data` conserva los datos locales.

## 2. Réplica del proveedor oficial

Desde `/credential-provider-mock`, levanta el servicio separado:

```powershell
docker compose up --build -d --wait
docker compose ps
Invoke-RestMethod http://localhost:8081/health
```

La réplica persiste revocaciones en un volumen Docker y reproduce los tres endpoints oficiales. Su API key local es ficticia; no reemplaces ese valor por una clave productiva. Los fixtures y el endpoint protegido de restauración se documentan en [`credential-provider-mock/README.md`](../credential-provider-mock/README.md).

## 3. Backend

En otra terminal, desde `/backend`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

El perfil importa opcionalmente `backend/.env`; Flyway migra o valida el esquema al iniciar y reCAPTCHA permanece deshabilitado. Comprueba ambas rutas:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/api/v1/system/status -Headers @{ "X-Correlation-ID" = "manual-local-check" }
```

La segunda respuesta debe ser `200`, contener backend/MySQL `UP` y devolver `X-Correlation-ID`.

El perfil local usa por defecto `CREDENTIAL_PROVIDER_MODE=real`, `CREDENTIAL_PROVIDER_BASE_URL=http://localhost:8081` y la misma API key ficticia de la réplica. Dentro del stack completo usa `http://host.docker.internal:8081`, permitido únicamente por el perfil local. El backend conserva `http://localhost:8080`, incluido el callback de ID Perú. Fuera de desarrollo loopback o Docker local, la URL del proveedor debe usar HTTPS; en producción el modo real y ambas credenciales son obligatorios. Nunca copies una clave real al repositorio ni a los logs.

La operación inicial `POST /api/v1/revocation-requests` no usa reCAPTCHA y consulta únicamente la existencia de credenciales disponibles. Su respuesta no contiene token, lista, cantidad, índice, fecha de creación ni UUID. Los DNI ficticios y resultados deterministas se documentan en [`credential-provider-mock/README.md`](../credential-provider-mock/README.md).

## 4. Contrato y frontend

Desde `/frontend`:

```powershell
npm ci
Copy-Item .env.example .env.local
npm run api:sync
npm run api:check
npm run test:integration
npm run dev
```

Abre `http://localhost:3000`. El formulario de inicio consume el contrato propio del backend y solo permite continuar cuando la existencia queda confirmada. El navegador también puede consultar `http://localhost:8080/api/v1/system/status`; CORS permite el origen local exacto y expone la correlación.

## reCAPTCHA deshabilitado

Local y producción utilizan `RECAPTCHA_MODE=disabled` y `NEXT_PUBLIC_RECAPTCHA_ENABLED=false`. El frontend no renderiza el widget ni envía evidencia ficticia, y el backend permite continuar sin `recaptchaToken`. No se requieren claves de Google en ninguno de estos dos ambientes.

## Verificación completa

Backend, desde `/backend`:

```powershell
.\mvnw.cmd clean verify
```

Frontend, desde `/frontend`, con backend activo para las dos primeras instrucciones:

```powershell
npm ci
npm run api:sync
npm run api:check
npm run typecheck
npm test
npm run test:integration
npm run build
```

## Apagado

Detén Next.js y Spring Boot con `Ctrl+C`. Desde `/backend`, detén MySQL conservando el volumen:

```powershell
docker compose down
```

Desde `/credential-provider-mock`, detén la réplica conservando sus credenciales mutadas:

```powershell
docker compose down
```

No uses `docker compose down -v` salvo que hayas confirmado que todos los datos locales son desechables. No confirmes ninguno de los archivos `.env`, el DNI personal, credenciales ni logs.
