# Revocación de credenciales digitales

## Arranque completo con Docker

Para ejecutar frontend, backend y MySQL solo se necesita Docker Desktop o Docker Engine con Compose. Desde la raíz del repositorio:

Antes del primer arranque crea `backend/.env` desde el ejemplo. El modo local usa ID Perú simulado por defecto y no requiere credenciales institucionales; para probar el servicio real configura `ID_PERU_MODE=real`, `ID_PERU_CLIENT_ID` e `ID_PERU_CLIENT_SECRET` con credenciales autorizadas.

```powershell
Copy-Item backend/.env.example backend/.env
# Opcional: edita backend/.env para seleccionar ID Perú real y completar sus credenciales.
```

El archivo se monta en `/app/.env` como solo lectura. Sus valores no se incorporan a la imagen ni aparecen expandidos en `docker compose config`.

```powershell
docker compose up --build -d --wait
docker compose ps
```

Servicios disponibles:

| Componente | Dirección |
|---|---|
| Frontend | `http://localhost:3000` |
| Backend | `http://localhost:8080` |
| Swagger local | `http://localhost:8080/swagger-ui.html` |
| MySQL | `localhost:3308` |

El backend puede iniciar aunque la réplica del proveedor no esté disponible. En ese estado la aplicación abre normalmente, pero una consulta de credenciales devuelve un error controlado hasta iniciar el proveedor.

Para consultar estado o logs:

```powershell
docker compose ps
docker compose logs -f backend frontend mysql
```

Para detener el stack conservando MySQL y las constancias:

```powershell
docker compose down
```

No uses `docker compose down -v` si necesitas conservar los datos.

## Proveedor de credenciales separado

La réplica del servicio externo no forma parte del stack principal. Se inicia y detiene independientemente:

```powershell
cd credential-provider-mock
docker compose up --build -d --wait
```

El proveedor queda publicado en `http://localhost:8081`. El backend Docker accede mediante `host.docker.internal`; el Compose principal incluye la equivalencia `host-gateway` necesaria en Linux.

## Desarrollo con solamente MySQL

El flujo existente continúa disponible para ejecutar backend y frontend directamente en la computadora:

```powershell
cd backend
Copy-Item .env.example .env
docker compose up -d --wait
```

El Compose del backend levanta exclusivamente MySQL en `3308`. Este modo y el stack completo son alternativas: antes de cambiar de uno a otro ejecuta `docker compose down` en la carpeta del modo activo para evitar un conflicto de puerto. Los volúmenes no se eliminan con ese comando.

## Requisitos y seguridad local

- No es necesario instalar Java, Node.js ni MySQL para el stack completo.
- Las credenciales de MySQL y del proveedor incluidas en el Compose raíz son ficticias; las credenciales de ID Perú solo se leen desde `backend/.env`.
- La configuración productiva no se incluye en estas imágenes ni acepta HTTP para el proveedor.
- reCAPTCHA permanece deshabilitado en local y producción.
