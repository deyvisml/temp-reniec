## Why

El repositorio ya dispone de una base backend, pero aún no cuenta con una aplicación web sobre la cual construir incrementalmente la experiencia ciudadana. Crear una base frontend mínima y verificable ahora separa el scaffolding, las convenciones de interfaz y la comunicación HTTP de las futuras tareas funcionales y visuales.

## What Changes

- Crear un proyecto npm único en `/frontend` con Node.js 24 LTS, Next.js 16.2.10, React 19.2.7, TypeScript y App Router.
- Configurar Tailwind CSS 4.3.2 mediante PostCSS, estilos globales mínimos, tipografía sans-serif de sistema, colores provisionales, espaciado básico y foco visible.
- Crear un layout raíz semántico en español con encabezado institucional textual, contenido principal, pie mínimo, contenedor adaptable, metadatos base y enlace para saltar al contenido.
- Crear una página inicial estrictamente temporal que indique que el sistema está en preparación, sin formularios, stepper ni lógica del flujo ciudadano.
- Añadir los archivos de App Router para carga, recurso no encontrado, error de segmento y error global, con mensajes seguros y sin detalles técnicos.
- Definir únicamente componentes compartidos que el layout utilice realmente; no crear carpetas `features`, rutas futuras ni capas vacías.
- Configurar `BACKEND_URL` como variable exclusiva de servidor y `NEXT_PUBLIC_APP_ENV` como identificador público no sensible, con ejemplo y documentación claros.
- Implementar un cliente HTTP nativo basado en `fetch` para JSON, errores de red o respuestas inválidas, lectura de `X-Correlation-ID` y `credentials: include`, sin llamadas funcionales reales.
- Añadir pruebas rápidas con Vitest y renderizado estático de React para página, layout, not-found y cliente HTTP, sin navegador, E2E ni servicios externos.
- Documentar requisitos, instalación, desarrollo, build, pruebas, variables de entorno y futura conexión con el backend existente.
- Mantener fuera del cambio las vistas definitivas, el flujo ciudadano, JWT, sesión, `localStorage`, gestores globales de estado, integraciones, diseño completo, administración y despliegue.

## Capabilities

### New Capabilities

- `frontend-foundation`: Define el proyecto frontend ejecutable, su shell accesible temporal, configuración de estilos y entorno, estados técnicos de App Router, cliente HTTP base, pruebas y documentación local.

### Modified Capabilities

Ninguna.

## Impact

- Se añadirá `/frontend` con código Next.js, configuración npm/TypeScript/Tailwind, pruebas y documentación local.
- La URL local documentada del backend será `http://localhost:8080`, coherente con su configuración existente; el frontend no realizará todavía solicitudes reales.
- No se modificarán `/backend`, `docs/`, las imágenes de referencia ni las especificaciones principales existentes.
- No se añadirán rutas ni componentes correspondientes a ingreso de DNI, autenticación, motivo, confirmación, revocación o constancia.
