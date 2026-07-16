## 1. Scaffold y configuración del proyecto

- [x] 1.1 Crear `/frontend` como paquete npm privado con Node.js 24 LTS, Next.js 16.2.10, React 19.2.7 y los scripts acordados
- [x] 1.2 Configurar TypeScript 6.0.2, tipos de Node/React y App Router con verificación estricta sin omitir errores de build
- [x] 1.3 Configurar Tailwind CSS 4.3.2 y `@tailwindcss/postcss` mediante PostCSS sin crear un `tailwind.config` innecesario
- [x] 1.4 Añadir Vitest 4.1.10 como única herramienta directa de pruebas y generar `package-lock.json`
- [x] 1.5 Revisar dependencias directas y eliminar linter, UI, HTTP, estado, formularios, DOM/E2E u otros paquetes fuera de alcance

## 2. Shell temporal y estilos

- [x] 2.1 Crear `app/globals.css` con Tailwind, normalización mínima, tipografía de sistema, pocos colores provisionales y foco visible
- [x] 2.2 Crear encabezado y pie textuales como únicos componentes compartidos, ambos consumidos por el layout
- [x] 2.3 Crear el layout raíz con `lang="es"`, metadatos, skip link, landmarks, contenedor adaptable y región global `aria-live`
- [x] 2.4 Crear la página inicial temporal con nombre provisional, estado técnico y ambiente público, sin controles ni llamadas del flujo ciudadano

## 3. Estados técnicos de App Router

- [x] 3.1 Crear `loading.tsx` con mensaje neutro y `role="status"`
- [x] 3.2 Crear `not-found.tsx` con explicación segura y enlace nativo a `/`
- [x] 3.3 Crear `error.tsx` y `global-error.tsx` con mensajes genéricos y reintento, sin renderizar detalles de excepción

## 4. Entorno y cliente HTTP

- [x] 4.1 Crear `.env.example` y exclusiones para `.env.local` con solo `BACKEND_URL` y `NEXT_PUBLIC_APP_ENV`
- [x] 4.2 Implementar `HttpClientError` y los tipos mínimos para resultado JSON y error compatible con el backend
- [x] 4.3 Implementar `requestJson<T>` sobre `fetch` con URL server-only, JSON, correlación, headers/opciones, `credentials: include` y errores genéricos
- [x] 4.4 Confirmar que no existen llamadas reales, autorización, JWT, refresh, reintentos, interceptores, cookies funcionales ni almacenamiento de sesión

## 5. Pruebas automatizadas

- [x] 5.1 Configurar Vitest en entorno Node sin jsdom, Testing Library, Playwright ni navegador
- [x] 5.2 Añadir pruebas de renderizado estático para página temporal, layout semántico y not-found
- [x] 5.3 Añadir pruebas del cliente HTTP para JSON y correlación, error estructurado, red, respuesta inválida y credenciales
- [x] 5.4 Ejecutar `npm run typecheck` y `npm test` sin backend, MySQL, servicios externos ni red

## 6. Documentación y verificación final

- [x] 6.1 Crear `frontend/README.md` con requisitos, npm, scripts, variables server/public, URL local del backend y límites de esta base
- [x] 6.2 Ejecutar una instalación limpia con `npm ci` y completar `npm run build` sin errores de TypeScript ni Tailwind
- [x] 6.3 Iniciar el build con `npm start`, verificar `/` y una ruta inexistente, y detener el proceso sin dejar servicios activos
- [x] 6.4 Revisar dependencias, árbol y rutas para confirmar que no se modificaron diseños ni backend y que no existen vistas funcionales, estado global, `localStorage`, JWT, administración o arquitectura anticipada
