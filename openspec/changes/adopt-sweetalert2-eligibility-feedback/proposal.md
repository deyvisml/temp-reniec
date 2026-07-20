## Why

La presentación actual de los resultados de elegibilidad depende de un modal desarrollado y mantenido dentro del proyecto. Para reducir código propio en aspectos delicados como comportamiento responsive, foco, teclado, superposición y compatibilidad entre navegadores, se adoptará una librería especializada, estable y mantenida.

## What Changes

- Incorporar SweetAlert2 como dependencia directa del frontend para presentar los resultados de la consulta de elegibilidad.
- Sustituir el componente modal nativo propio por una integración pequeña y específica que traduzca los estados existentes a opciones de SweetAlert2.
- Conservar la lógica tipada que diferencia resultados elegibles, no elegibles, inconclusos y errores controlados.
- Mantener acciones seguras y neutrales: continuar, reintentar, volver al inicio o aceptar el resultado según corresponda, sin sugerir la prueba sucesiva de otros DNIs.
- Configurar SweetAlert2 para respetar la identidad visual, el comportamiento responsive, los objetivos táctiles y la reducción de movimiento del proyecto.
- Mantener el formulario montado como contexto y evitar resultados incrustados, tarjetas sobredimensionadas o alertas efímeras.
- Eliminar el modal propio y sus pruebas específicas una vez cubierta la misma conducta mediante la librería.
- No incorporar un kit de componentes adicional ni crear una abstracción genérica para alertas que todavía no tenga otros consumidores.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `citizen-eligibility-entry`: los resultados separados de elegibilidad serán presentados mediante SweetAlert2, una dependencia especializada y mantenida, en lugar de un modal implementado dentro del proyecto.

## Impact

- Frontend: `package.json`, lockfile, formulario de elegibilidad, componente/presentador de resultados, carga de estilos y pruebas.
- Dependencias: se añade `sweetalert2` como única dependencia de ejecución nueva para este cambio.
- Sin cambios en endpoints, contratos OpenAPI, backend, MySQL, estados funcionales ni rutas ciudadanas.
- La decisión sustituye la restricción del cambio previo que prohibía utilizar una librería de alertas, pero conserva sus requisitos funcionales, visuales y de accesibilidad.
