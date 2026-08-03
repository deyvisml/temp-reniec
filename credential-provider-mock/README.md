# Réplica local del proveedor de credenciales

Servicio exclusivo para desarrollo que reproduce los contratos oficiales de disponibilidad, listado y revocación. Escucha en `http://localhost:8081`; el backend institucional continúa en `http://localhost:8080`.

## Arranque con Docker

```powershell
Copy-Item .env.example .env
# Agrega tu DNI únicamente en .env si probarás ID Perú real.
docker compose up --build -d --wait
docker compose ps
```

`PERSONAL_TEST_DNI` es opcional, debe contener ocho dígitos y nunca debe confirmarse en Git. Al configurarlo se generan dos credenciales vigentes y una revocada. Los cambios se conservan en el volumen `credential-provider-mock-local_credential-provider-data`.

El healthcheck público está disponible en:

```powershell
Invoke-RestMethod http://localhost:8081/health
```

Todos los POST requieren `x-api-key`. El valor local predeterminado es ficticio:

```powershell
$headers = @{ "x-api-key" = "local-credential-provider-key" }
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

`has-credentials` responde `true` solo cuando queda al menos una credencial vigente. Una revocación modifica el JSON persistente y una repetición sobre la misma tupla responde exitosamente como operación idempotente.

Para restaurar todos los fixtures y regenerar el DNI personal actual:

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
