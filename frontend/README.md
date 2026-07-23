# Frontend — Cancelación de certificados digitales

Frontend ciudadano en Next.js 16, React, TypeScript, Tailwind CSS y App Router. `/cancelacion` es la entrada del trámite. En desarrollo local, el paso 1 de autenticación se presenta y permanece en `/autorizacion` porque ese es el origen registrado para las credenciales de prueba de ID Perú; en producción el paso permanece en `/cancelacion`.

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
| `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` | Navegador y servidor | Clave pública aprobada | Site key de Google reCAPTCHA v2 Checkbox, incorporada al bundle durante el build. |

Las variables `NEXT_PUBLIC_*` quedan incorporadas al bundle al compilar y nunca deben contener secretos. La site key es pública; la secret permanece exclusivamente en el backend. `.env.local` está ignorado por Git. Si la site key falta, el build sigue funcionando pero el formulario queda bloqueado con un mensaje controlado.

## Contrato OpenAPI y tipos

Con MySQL y backend local activos, OpenAPI está en `http://localhost:8080/v3/api-docs`. La documentación navegable puede consultarse en `http://localhost:8080/swagger-ui.html`:

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

Usa los DNI ficticios documentados en el README del backend para reproducir cada resultado. El botón solo se habilita con DNI válido, token actual y ningún envío en curso. DNI y token no se guardan en almacenamiento web ni URL. Solo un resultado disponible permite avanzar: con `NEXT_PUBLIC_APP_ENV=local` el navegador pasa de `/cancelacion` a `/autorizacion`; en producción la vista cambia dentro de `/cancelacion`. La continuidad viaja exclusivamente en una cookie HttpOnly y el frontend no puede leerla ni persistirla.

`/`, `/verificacion-identidad` y `/verificacion-identidad/retorno` existen únicamente como redirecciones hacia `/cancelacion`. `/autorizacion` renderiza el mismo componente compartido del flujo cuando el ambiente es local y redirige a `/cancelacion` fuera de local. Ningún DNI, `requestId`, certificado o resultado de autenticación se transporta en la URL. Una recarga consulta solo el contexto temporal vigente del backend; no recupera solicitudes terminadas ni progreso histórico.

## Cliente HTTP

`lib/http-client.ts` centraliza `fetch`, JSON, cookies futuras, correlación, timeout de ocho segundos, cancelación y errores seguros. No incorpora JWT, reintentos, interceptores, sesión, almacenamiento ni librerías HTTP externas.

La portada se basa en `docs/ui-reference/home.png` y el paso 1 en `docs/ui-reference/step-1.png`, corrigiendo el stepper a cinco pasos. El contexto funcional prevalece sobre cualquier detalle visual contradictorio. El listado del paso 2, motivo, confirmación, revocación, constancia y despliegue productivo permanecen fuera de alcance.
