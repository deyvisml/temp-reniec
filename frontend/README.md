# Frontend — Cancelación de certificados digitales

Frontend ciudadano en Next.js 16, React, TypeScript, Tailwind CSS y App Router. La página de inicio valida el DNI y consulta la elegibilidad mediante el backend; las etapas posteriores todavía no están implementadas.

## Requisitos e inicio

- Node.js 24 LTS y npm 10.
- Backend y MySQL solo son necesarios para sincronizar contratos, ejecutar la suite real o ver el estado disponible; el frontend puede iniciar sin ellos.

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Abre `http://localhost:3000`. La portada permanece visible si el backend está detenido y muestra un error comprensible únicamente después de enviar la consulta.

## Variables

| Variable | Exposición | Valor local | Propósito |
| --- | --- | --- | --- |
| `BACKEND_URL` | Solo servidor y herramientas | `http://localhost:8080` | URL del backend para ejecución servidor, sincronización y pruebas reales. |
| `NEXT_PUBLIC_BACKEND_URL` | Navegador y servidor | `http://localhost:8080` | Dirección pública utilizada por la consulta ciudadana. |
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

## Convención de estilos

Tailwind CSS es la estrategia principal de presentación. El layout, espaciado, tipografía, colores, responsive y estados visuales se expresan con utilidades literales colocadas en el componente TSX que posee el markup.

`app/globals.css` se limita a importar Tailwind y a declarar un tema global mínimo. No debe contener selectores de páginas o componentes, `@apply` para ocultar conjuntos de utilidades, estilos visuales inline ni una segunda hoja global. Si Tailwind no puede representar razonablemente una necesidad vigente, la excepción debe ser la regla CSS más pequeña posible, quedar documentada junto a su motivo y no establecer otra estrategia de estilos.

Las clases condicionales deben escribirse como literales completos para que Tailwind pueda detectarlas. No se construyen fragmentos como `text-${color}-700`. El proyecto no usa `clsx`, `tailwind-merge`, CSS Modules, Sass, styled-components ni un kit de UI porque las variantes actuales no justifican esas dependencias.

La suite real usa el cliente central, los tipos generados y un backend conectado a MySQL:

```powershell
npm run test:integration
```

Consulta [`docs/LOCAL_INTEGRATION.md`](../docs/LOCAL_INTEGRATION.md) para el orden completo de inicio y apagado. La URL API base es `/api/v1`; CORS local admite exactamente `http://localhost:3000`.

## Consulta local

Usa los DNI ficticios documentados en el README del backend para reproducir cada resultado. El formulario no guarda el DNI en almacenamiento web ni lo coloca en la URL. Solo un resultado elegible muestra la transición preparada hacia verificación de identidad mediante `requestId`; este identificador no autentica ni autoriza y la pantalla de ID Perú pertenece a una tarea posterior.

## Cliente HTTP

`lib/http-client.ts` centraliza `fetch`, JSON, cookies futuras, correlación, timeout de ocho segundos, cancelación y errores seguros. No incorpora JWT, reintentos, interceptores, sesión, almacenamiento ni librerías HTTP externas.

La portada se basa en `docs/ui-reference/home.png` y el contexto funcional prevalece sobre cualquier detalle visual contradictorio. JWT, ID Perú, motivo, confirmación, revocación, constancia y despliegue productivo permanecen fuera de alcance.
