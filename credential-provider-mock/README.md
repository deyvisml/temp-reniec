# Réplica local del proveedor de credenciales

Servicio exclusivo para desarrollo que reproduce los contratos oficiales de disponibilidad, listado y revocación. Escucha en `http://localhost:8081`; el backend institucional continúa en `http://localhost:8080`.

## Arranque con Docker

```powershell
docker compose up --build -d --wait
docker compose ps
```

No se requiere configurar variables de DNI. Los datos iniciales están versionados en `fixtures/credentials.seed.json` y los cambios se conservan en el volumen `credential-provider-mock-local_credential-provider-data`.

El healthcheck público está disponible en:

```powershell
Invoke-RestMethod http://localhost:8081/health
```

Todos los POST requieren `x-api-key`. El valor local predeterminado es ficticio:

```powershell
$headers = @{ "x-api-key" = "app_revocaciones_reniec.RENIEC2026" }
Invoke-RestMethod http://localhost:8081/api/v1/list-credentials `
  -Method Post -ContentType application/json -Headers $headers `
  -Body '{"dni":"00000022"}'
```

## Fixtures

| DNI | Escenario inicial |
|---|---|
| `00000001` | Dos vigentes y una revocada |
| `00000020` | Sin credenciales |
| `00000021` | Una vigente |
| `00000022` | Una vigente y una revocada |
| `00000028` | Únicamente una revocada |
| `73905791` | Dos vigentes y una revocada |
| `42992664` | Cuatro vigentes y una revocada |

`has-credentials` responde `true` cuando existe al menos una credencial registrada, ya sea vigente o revocada; responde `false` únicamente cuando el DNI no tiene credenciales. El fixture `00000001` incluye dos credenciales vigentes con el mismo UUID e índices distintos para reproducir el contrato real. Una revocación modifica el JSON persistente y una repetición sobre la misma tupla responde exitosamente como operación idempotente.

Para restaurar todos los fixtures desde el JSON versionado:

```powershell
Invoke-RestMethod http://localhost:8081/__admin/reset `
  -Method Post -ContentType application/json -Headers $headers -Body '{}'
```

## Ejecución sin Docker

Requiere Node.js 24:

```powershell
npm ci
npm test
npm run typecheck
npm run dev
```

El servicio no debe utilizarse en producción ni recibir una API key productiva.
