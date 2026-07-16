# Frontend — Cancelación de certificados digitales

Base técnica temporal del frontend. Incluye el shell de Next.js, estados generales y un cliente HTTP reutilizable; todavía no implementa el flujo ciudadano ni reproduce las vistas de referencia.

## Requisitos

- Node.js 24 LTS
- npm 10 o una versión compatible con Node.js 24

## Preparación y ejecución

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

La aplicación queda disponible en `http://localhost:3000`.

Comandos principales:

```powershell
npm run typecheck
npm test
npm run build
npm start
```

## Variables de entorno

| Variable | Exposición | Valor local | Propósito |
| --- | --- | --- | --- |
| `BACKEND_URL` | Solo servidor | `http://localhost:8080` | URL base reservada para futuras llamadas al backend. |
| `NEXT_PUBLIC_APP_ENV` | Navegador y servidor | `local` | Identifica el ambiente mostrado por la página temporal. |

Solo las variables con el prefijo `NEXT_PUBLIC_` se incorporan al código enviado al navegador. No deben almacenarse secretos en variables públicas ni confirmarse archivos `.env.local` en el repositorio.

El backend puede ejecutarse en el puerto `8080`, pero esta base no realiza llamadas reales y las pruebas no dependen de él. La sesión, JWT, cookies funcionales, CORS o una posible mediación del servidor de Next.js se definirán cuando exista un caso de integración concreto.

La página y los colores actuales son provisionales. Las vistas funcionales se implementarán después a partir de `docs/context/PROJECT_CONTEXT.md` y `docs/ui-reference/README.md`; el despliegue productivo también queda fuera del alcance de esta base.
