# Integración local: MySQL → backend → frontend

Este recorrido levanta las tres capas sin contenerizar backend ni frontend. Usa MySQL 8.4 en Docker, publicado en `3307`, Spring Boot en `8080` y Next.js en `3000`.

## 1. MySQL

Desde `/backend`, crea una sola vez el archivo privado y levanta la base:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env config
docker compose up -d --wait
docker compose ps
```

El resultado debe mostrar `3307->3306` y estado saludable. Compose crea la base y usuario; no ejecutes SQL manual. El volumen `cancelacion-certificados-local_mysql-data` conserva los datos locales.

## 2. Backend

En otra terminal, desde `/backend`:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

El perfil importa opcionalmente `backend/.env`; Flyway migra o valida el esquema al iniciar y el adaptador Google exige `RECAPTCHA_SECRET_KEY`. Comprueba ambas rutas:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/api/v1/system/status -Headers @{ "X-Correlation-ID" = "manual-local-check" }
```

La segunda respuesta debe ser `200`, contener backend/MySQL `UP` y devolver `X-Correlation-ID`.

La operación inicial `POST /api/v1/cancellation-requests` valida reCAPTCHA antes de cualquier escritura y consulta únicamente la existencia de certificados disponibles. Su respuesta no contiene token, lista, cantidad, número de orden, fecha de creación ni UUID. Los DNI ficticios y resultados deterministas del servicio de disponibilidad se documentan en [`backend/README.md`](../backend/README.md).

## 3. Contrato y frontend

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

## Prueba manual de reCAPTCHA v2 Checkbox

El proyecto institucional de referencia usa `@google-recaptcha/react` 2.4.2, verificación server-to-server con `siteverify` y el par oficial de pruebas publicado por Google. En este proyecto se adaptaron esos tres elementos a Next.js/Spring Boot, pero no se copiaron su estado global, React Query, circuit breaker ni modo de bypass frontend.

Para probar manualmente, coloca la site key oficial de pruebas en `frontend/.env.local` como `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` y su secret correspondiente en `backend/.env` como `RECAPTCHA_SECRET_KEY`. Ambas rutas están ignoradas y los valores no deben agregarse a `.env.example`, código, capturas o commits. Configura localmente `RECAPTCHA_ALLOWED_HOSTNAMES=localhost,testkey.google.com`, reinicia ambas aplicaciones, completa la casilla y envía un DNI ficticio. Las claves oficiales de prueba reportan `testkey.google.com` como hostname de la verificación, aunque el formulario se ejecute en `localhost`; Google también muestra una advertencia visual indicando que la clave es de prueba.

Verifica con teclado en anchos móvil y escritorio: foco visible en la casilla, botón bloqueado antes del desafío, un solo envío, y widget nuevo después de cada resultado o error. Las claves oficiales de prueba aceptan desafíos de desarrollo y están expresamente prohibidas en producción. `testkey.google.com` también debe limitarse al entorno local; producción requiere claves propias restringidas al dominio institucional y una allowlist externa que contenga únicamente los hostnames reales.

Esta integración no agrega migraciones, tablas, columnas ni repositorios: la evidencia anti-bot es efímera y nunca llega a MySQL.

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

No uses `docker compose down -v` salvo que hayas confirmado que todos los datos locales son desechables. No confirmes `backend/.env`, `frontend/.env.local`, credenciales ni logs.
