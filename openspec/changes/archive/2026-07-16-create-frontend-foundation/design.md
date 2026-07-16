## Context

El repositorio contiene las fuentes funcionales y visuales permanentes, una decisión explícita de usar Next.js y Tailwind CSS, y un backend Spring Boot ejecutable en `http://localhost:8080`. El backend expone actualmente solo `/actuator/health`, devuelve `X-Correlation-ID` y usa un contrato JSON de error con `code`, `message`, `timestamp`, `path` y `correlationId`. No existen todavía APIs ciudadanas, JWT, persistencia ni contratos externos que el frontend pueda consumir.

Esta tarea debe crear una aplicación útil para desarrollo desde el primer commit, pero deliberadamente neutral respecto de las pantallas finales. Las imágenes en `docs/ui-reference/` siguen siendo referencias futuras: no se copiarán, reinterpretarán ni implementarán en este cambio.

## Goals / Non-Goals

**Goals:**

- Crear un proyecto Next.js único, reproducible, compilable y ejecutable bajo `/frontend`.
- Establecer App Router, TypeScript, Tailwind, configuración de entorno y un shell semántico mínimo.
- Cubrir estados técnicos de carga, not-found y errores sin exponer detalles internos.
- Preparar una única abstracción HTTP pequeña y probada sobre `fetch` para uso futuro.
- Proporcionar pruebas rápidas de renderizado estático y cliente HTTP sin navegador ni backend activo.
- Dejar una estructura que pueda crecer por funcionalidades cuando aparezcan tareas reales.

**Non-Goals:**

- Implementar o aproximar visualmente las pantallas `home`, `step-1`, `step-2`, `step-3` o `step-4-final`.
- Crear ingreso o validación de DNI, consulta de certificados, stepper, ID Perú, motivos, confirmación, revocación o constancia.
- Añadir JWT, refresh tokens, sesión, recuperación de progreso, `localStorage`, cookies funcionales o llamadas reales.
- Incorporar Redux, Zustand, Axios, interceptores, reintentos, catálogo completo de errores, E2E o un sistema de diseño.
- Crear rutas, componentes genéricos, carpetas `features` o recursos estáticos antes de que tengan un consumidor.
- Modificar `/backend`, configurar despliegue productivo o añadir módulos administrativos.

## Decisions

### Versiones, runtime y lockfile

Se fijarán Node.js 24 LTS, Next.js 16.2.10, React y React DOM 19.2.7, TypeScript 6.0.2, Tailwind CSS y `@tailwindcss/postcss` 4.3.2, y Vitest 4.1.10. Next.js 16.2 es la línea estable vigente y 16.3 permanece en preview. Node 24 es LTS y supera el mínimo 20.9 exigido por Next 16. `package-lock.json` se versionará y CI/desarrollo reproducible usarán `npm ci`.

Fuentes de versión: [Next.js 16.2](https://nextjs.org/blog/next-16-2), [Next.js installation](https://nextjs.org/docs/app/getting-started/installation), [React versions](https://react.dev/versions), [Tailwind CSS](https://tailwindcss.com/blog), [Node.js releases](https://nodejs.org/en/about/previous-releases) y [Vitest](https://www.npmjs.com/package/vitest).

Se usará npm porque acompaña al runtime elegido y no existe un package manager previo que conservar. No se añadirá ESLint/Biome en esta tarea: `next build`, `tsc --noEmit` y las pruebas cubren las verificaciones exigidas sin introducir otra cadena de plugins. Un cambio posterior podrá adoptar una política de linting institucional.

### Scaffold y dependencias mínimas

El proyecto será un único paquete npm privado con scripts `dev`, `build`, `start`, `typecheck`, `test` y `test:watch`. Dependencias productivas directas: `next`, `react` y `react-dom`. Dependencias de desarrollo: TypeScript y tipos correspondientes, `tailwindcss`, `@tailwindcss/postcss` y `vitest`. No se añadirán librerías de UI, iconos, formularios, estado, HTTP, validación, fuentes ni testing DOM.

Se configurará `engines.node` para Node 24 LTS. `create-next-app` podrá usarse como generador mecánico, pero el resultado se reducirá a este conjunto y se revisará el lockfile.

### App Router y estructura real

La estructura inicial contendrá únicamente:

- `app/` con `layout.tsx`, `page.tsx`, `loading.tsx`, `not-found.tsx`, `error.tsx`, `global-error.tsx` y `globals.css`.
- `components/` con encabezado y pie usados por el layout.
- `lib/http-client.ts` con el cliente HTTP y su error tipado.
- `tests/` con las pruebas de renderizado y cliente HTTP.

No se crearán `features/`, `styles/` ni `public/` vacíos. Los estilos globales vivirán junto al App Router y las futuras funcionalidades crearán su propia carpeta cuando exista código real.

### Layout y página temporal

El layout raíz declarará `<html lang="es">`, metadatos estáticos, enlace de salto al contenido, encabezado institucional solo textual, `<main id="main-content">`, región `aria-live` para futuros mensajes globales y pie mínimo. El contenedor tendrá ancho máximo y padding fluido, pero no reproducirá el layout de los prototipos.

La página `/` será un Server Component estático. Mostrará el nombre provisional, el texto “Proyecto en preparación”, un estado técnico y `NEXT_PUBLIC_APP_ENV`; no contendrá inputs, acciones del flujo, stepper, navegación futura ni consulta al backend.

Se consideró usar imágenes RENIEC o reconstruir el encabezado de los diseños, pero los recursos oficiales y lineamientos visuales aún no están aprobados para implementación. El texto simple evita inventar activos o decisiones visuales.

### Tailwind y estilos globales mínimos

Tailwind 4 se integrará mediante `@tailwindcss/postcss` y `@import "tailwindcss"` en `app/globals.css`, sin `tailwind.config` porque no se necesita personalización programática. El CSS global definirá `box-sizing`, fondo, texto, fuente de sistema (`Arial`, `Helvetica Neue`, sans-serif), unos pocos colores provisionales, ancho de lectura y `:focus-visible` contrastante.

Las clases Tailwind en layout y página demostrarán que la compilación funciona. No se crearán tokens extensos, variantes, componentes visuales o paleta institucional definitiva. Los nombres y valores provisionales no se presentarán como lineamientos RENIEC.

### Accesibilidad inicial

Además de idioma, landmarks y título, el shell tendrá jerarquía de encabezados, enlace de salto visible al recibir foco, foco no dependiente solo del color, contraste legible, controles nativos con etiquetas textuales y mensajes de carga con `role="status"`. No se añadirán animaciones; por tanto no se requiere todavía una capa de preferencias de movimiento.

Las pruebas estáticas verificarán los landmarks, `lang="es"`, contenido principal y textos esenciales. La auditoría completa con navegador y tecnologías asistivas queda fuera de alcance.

### Estados técnicos de App Router

`loading.tsx` mostrará un mensaje neutro con `role="status"`. `not-found.tsx` ofrecerá un título, explicación y enlace nativo a `/`. `error.tsx` y `global-error.tsx` serán Client Components con mensajes genéricos y una acción de reintento; no renderizarán `error.message`, digest, stack ni datos técnicos. `global-error.tsx` incluirá sus propios `<html lang="es">` y `<body>` según la convención de Next.

No se creará un catálogo de códigos ni un sistema global de notificaciones.

### Variables de entorno

Se documentarán solo dos variables en `.env.example`:

- `BACKEND_URL=http://localhost:8080`: exclusiva de servidor y usada por el cliente HTTP.
- `NEXT_PUBLIC_APP_ENV=local`: etiqueta pública no sensible mostrada por la página temporal.

La documentación explicará que únicamente los nombres `NEXT_PUBLIC_*` se incorporan al bundle del navegador y nunca deben contener secretos. `.env.local` permanecerá ignorado. No se añadirá una URL pública del backend hasta que se decida si la arquitectura usará acceso directo, proxy o BFF.

### Cliente HTTP nativo

`lib/http-client.ts` expondrá `requestJson<T>` y `HttpClientError`. El cliente construirá URLs relativas contra `BACKEND_URL`, enviará `Accept: application/json`, respetará headers/opciones del llamador, fijará `credentials: "include"` como preparación pasiva para cookies y no añadirá autorización.

Una respuesta exitosa deberá ser JSON y se devolverá como `{ data, correlationId }`, leyendo `X-Correlation-ID`. En respuestas no exitosas, el cliente intentará leer de forma defensiva el contrato de error del backend y lanzará `HttpClientError` con código público, status y correlación; si el cuerpo no es JSON válido usará un mensaje genérico. Fallos de red producirán `NETWORK_ERROR` y JSON exitoso inválido producirá `INVALID_RESPONSE`. Ningún mensaje técnico del motor de `fetch` se expondrá como mensaje público.

No habrá interceptores, reintentos, refresh, almacenamiento, cookies inventadas ni llamadas al endpoint de salud. La abstracción será runtime-neutral, pero la única URL configurada en esta tarea es server-only.

### Pruebas sin DOM ni E2E

Vitest ejecutará en entorno Node. `react-dom/server` renderizará página, layout y not-found a HTML estático, evitando jsdom y Testing Library. Las pruebas del cliente reemplazarán `fetch` para cubrir JSON exitoso, correlación, error del backend, fallo de red, respuesta inválida y `credentials: include`.

`npm run typecheck`, `npm test` y `npm run build` serán validaciones separadas. Finalmente se iniciará `next start` sobre el build, se consultarán `/` y una ruta inexistente y se detendrá el proceso, sin depender del backend.

### Documentación local

`frontend/README.md` cubrirá Node 24 LTS, `npm ci`, scripts, `.env.local`, clasificación server/public, URL del backend existente y la ausencia deliberada de integración funcional. No duplicará el contexto de dominio ni describirá pantallas futuras.

## Risks / Trade-offs

- [Versiones recientes podrían revelar incompatibilidades de tooling] → Fijar versiones y lockfile, ejecutar typecheck, pruebas, build y arranque real durante la implementación.
- [Una página temporal podría confundirse con el diseño final] → Etiquetarla claramente como preparación y evitar logos, formularios, stepper o composición de los prototipos.
- [Una variable pública podría usarse para secretos] → Limitarla a la etiqueta de ambiente y documentar explícitamente la semántica `NEXT_PUBLIC_*`.
- [El cliente HTTP podría crecer prematuramente] → Mantener una sola función, una clase de error y fetch nativo; ampliar solo al aparecer contratos reales.
- [El uso de `credentials: include` podría interpretarse como sesión implementada] → Documentarlo únicamente como compatibilidad futura; no definir cookies, JWT ni almacenamiento.
- [Las pruebas estáticas no validan accesibilidad o navegación reales] → Cubrir solo la base semántica ahora y reservar E2E/auditoría para una tarea específica.
- [Los estilos provisionales podrían convertirse accidentalmente en definitivos] → Mantener pocos valores y señalar que las referencias visuales y lineamientos aprobados gobernarán la implementación posterior.

## Migration Plan

1. Generar `/frontend` con npm, las versiones fijadas, App Router, TypeScript y lockfile.
2. Reducir dependencias y estructura al conjunto acordado; configurar Tailwind y TypeScript.
3. Implementar layout, página temporal, estados técnicos y estilos accesibles mínimos.
4. Añadir variables de entorno y el cliente HTTP sin llamadas reales.
5. Incorporar pruebas de renderizado y cliente, más documentación local.
6. Ejecutar `npm ci`, `npm run typecheck`, `npm test`, `npm run build`, iniciar el build y verificar `/` y not-found.

No existe migración de datos ni despliegue. La reversión consiste en retirar `/frontend`, porque esta base aún no tiene consumidores, sesión ni integración funcional.

## Open Questions

Ninguna para la base técnica. CORS/BFF, cookies reales, JWT, URLs productivas, assets oficiales y diseño responsive definitivo se decidirán cuando existan contratos y tareas específicas.
