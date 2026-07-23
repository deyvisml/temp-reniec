## Why

El retorno real de ID Perú v1 llega al callback mediante HTTP GET, pero el backend actual solo admite POST; por ello el navegador recibe `METHOD_NOT_ALLOWED` y queda expuesto a una respuesta JSON técnica en lugar de regresar al flujo ciudadano. El callback debe completarse como una frontera navegador-backend: procesar el resultado una sola vez, redirigir siempre a una ruta frontend controlada y permitir que la interfaz resuelva el siguiente estado desde el contexto validado por el backend.

## What Changes

- Aceptar callbacks de ID Perú por GET y POST en la misma ruta registrada, manteniendo compatibilidad con las variantes del proveedor y con el proyecto institucional de referencia.
- Procesar `code`, `state`, `session_state` y `error` con las validaciones, consumo único y protección ya definidas, sin exponerlos en la redirección al frontend.
- Responder al navegador con una redirección HTTP controlada hacia la ruta frontend configurada tanto en éxito como en fallos esperados, evitando mostrar el formato JSON general de errores en el callback del proveedor.
- Hacer que el frontend consulte el estado temporal validado por el backend después del retorno: en éxito avanzará a una presentación mínima del paso 2; en cancelación, rechazo, expiración o error permanecerá en el paso 1 y mostrará un único aviso accesible con una acción de reintento válida.
- Actualizar OpenAPI, documentación y pruebas para reflejar los métodos aceptados, la redirección y el comportamiento visible del retorno.
- No implementar todavía el listado ni la selección funcional de certificados del paso 2.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `id-peru-citizen-authentication`: el callback deja de estar restringido a POST, acepta el retorno GET utilizado por ID Perú y transforma todos los resultados procesables en una redirección segura al frontend.
- `single-cancellation-flow-route`: después del callback, el frontend resuelve el resultado desde el contexto temporal del backend; muestra el paso 2 mínimo al autenticarse o conserva el paso 1 con un aviso controlado cuando falla.

## Impact

- Backend: controlador de autenticación ID Perú, manejo del callback, redirecciones, cookie temporal, documentación OpenAPI y pruebas web/de integración.
- Frontend: orquestación de `/cancelacion` y `/autorizacion` local, resolución del estado tras el retorno, presentación mínima del paso 2 y avisos de error con SweetAlert2 ya instalado.
- Contrato externo: se conserva `http://localhost:8080/api/v1/idperu/callback` como callback local registrado; no se añaden credenciales, parámetros sensibles ni nuevas dependencias.
- Persistencia: se reutilizan los intentos de verificación y la autorización temporal existentes; no se crean tablas ni migraciones nuevas salvo que una corrección comprobada de integridad lo requiera.
