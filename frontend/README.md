# Frontend — Cancelación de certificados digitales

Base técnica temporal en Next.js 16, React, TypeScript, Tailwind CSS y App Router. La página comprueba la integración con backend y MySQL, pero no implementa controles del flujo ciudadano.

## Requisitos e inicio

- Node.js 24 LTS y npm 10.
- Backend y MySQL solo son necesarios para sincronizar contratos, ejecutar la suite real o ver el estado disponible; el frontend puede iniciar sin ellos.

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Abre `http://localhost:3000`. Si el backend está detenido, la página permanece utilizable, muestra “Integración no disponible” y permite reintentar manualmente; no realiza polling.

## Variables

| Variable | Exposición | Valor local | Propósito |
| --- | --- | --- | --- |
| `BACKEND_URL` | Solo servidor y herramientas | `http://localhost:8080` | URL del backend para ejecución servidor, sincronización y pruebas reales. |
| `NEXT_PUBLIC_BACKEND_URL` | Navegador y servidor | `http://localhost:8080` | Dirección pública utilizada por el indicador cliente. |
| `NEXT_PUBLIC_APP_ENV` | Navegador y servidor | `local` | Etiqueta pública del ambiente. |

Las variables `NEXT_PUBLIC_*` quedan incorporadas al bundle al compilar y nunca deben contener secretos. `.env.local` está ignorado por Git.

## Contrato OpenAPI y tipos

Con MySQL y backend local activos, OpenAPI está en `http://localhost:8080/v3/api-docs`:

```powershell
npm run api:sync
npm run api:check
```

`api:sync` actualiza `openapi/backend-api.json` y `lib/api/generated.ts`. Ambos se versionan y no se editan manualmente. `api:check` falla si el backend actual, la copia canónica o los tipos difieren. El código usa aliases mínimos en `lib/api/contracts.ts`, no un SDK duplicado.

## Verificaciones

```powershell
npm run typecheck
npm test
npm run build
```

Estas verificaciones usan Node y dobles de `fetch`; no requieren backend, MySQL, jsdom ni navegador.

La suite real usa el cliente central, los tipos generados y un backend conectado a MySQL:

```powershell
npm run test:integration
```

Consulta [`docs/LOCAL_INTEGRATION.md`](../docs/LOCAL_INTEGRATION.md) para el orden completo de inicio y apagado. La URL API base es `/api/v1`; CORS local admite exactamente `http://localhost:3000`.

## Cliente HTTP

`lib/http-client.ts` centraliza `fetch`, JSON, cookies futuras, correlación, timeout de ocho segundos, cancelación y errores seguros. No incorpora JWT, reintentos, interceptores, sesión, almacenamiento ni librerías HTTP externas.

La página, colores y estado técnico son temporales. Las vistas funcionales se implementarán después desde las referencias aprobadas; el despliegue productivo permanece fuera de alcance.
