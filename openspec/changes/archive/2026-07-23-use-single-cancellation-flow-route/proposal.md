## Why

El flujo ciudadano está fragmentado actualmente entre `/`, `/verificacion-identidad` y `/verificacion-identidad/retorno`, aunque todas esas vistas pertenecen al mismo trámite. Esta segmentación expone detalles del paso actual, obliga a coordinar navegación entre rutas y se volverá más difícil de mantener cuando se incorporen los pasos restantes.

## What Changes

- Establecer `/cancelacion` como URL canónica única para todo el flujo ciudadano: consulta inicial, autenticación, selección, motivo, confirmación y constancia.
- Renderizar la vista correspondiente dentro de esa ruta según el estado vigente del trámite, sin incorporar el paso, DNI, identificador de solicitud ni datos de certificados en la URL.
- Cambiar el retorno de ID Perú para volver a `/cancelacion`; el resultado se resolverá mediante la autorización temporal y el estado consultado al backend, no mediante una ruta o parámetro visible.
- Hacer que `/` conduzca a `/cancelacion` y retirar el uso funcional de las rutas específicas `/verificacion-identidad` y `/verificacion-identidad/retorno`.
- Mantener redirecciones internas controladas desde las rutas anteriores hacia `/cancelacion` únicamente para evitar enlaces rotos durante la transición; la barra del navegador debe terminar siempre en la URL canónica.
- Centralizar la constante de ruta y las decisiones de navegación del flujo en el frontend.
- Actualizar pruebas y documentación para exigir que el avance entre pasos no cambie la URL visible.
- No introducir recuperación histórica, sesión permanente, parámetros de paso ni un gestor global de estado.
- **BREAKING**: las rutas frontend específicas de autenticación dejan de ser destinos canónicos y el retorno configurado de ID Perú cambia a la ruta única.

## Capabilities

### New Capabilities

- `single-cancellation-flow-route`: Define la ruta canónica `/cancelacion`, la resolución interna de vistas y la conservación de una URL estable durante todo el trámite ciudadano.

### Modified Capabilities

- `citizen-eligibility-entry`: Sustituye la navegación a `/verificacion-identidad` por la continuación interna hacia el paso de autenticación dentro de `/cancelacion`, sin exponer datos ni cambiar la URL.

## Impact

- Frontend Next.js: estructura de `app`, composición del flujo, navegación de la consulta inicial, retorno de identidad, estados de error y pruebas de renderizado/navegación.
- Backend Spring Boot: URL fija de retorno al frontend y configuración externa relacionada con ID Perú; no cambian los endpoints funcionales ni el protocolo OAuth/OIDC.
- OpenAPI y tipos generados solo si algún contrato documentado contiene rutas de retorno frontend.
- Documentación de ejecución, variables de entorno, integración ID Perú y flujo ciudadano.
- No afecta el modelo de datos, las migraciones, la consulta de certificados ni las reglas de negocio de cancelación.
