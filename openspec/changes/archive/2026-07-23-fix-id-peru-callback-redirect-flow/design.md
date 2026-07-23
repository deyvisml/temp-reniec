## Context

La integración ya construye la autorización, valida `state` y PKCE, intercambia tokens y persiste el intento. El defecto está en la frontera HTTP: `IdentityVerificationController` declara el callback exclusivamente como POST con `application/x-www-form-urlencoded`, mientras ID Perú v1 y el proyecto `sistema-autorizacion-certificados-reniec` retornan al navegador mediante GET con parámetros de consulta. Spring rechaza la petición antes de ejecutar el caso de uso y el navegador muestra el error JSON global.

El proyecto de referencia admite GET y POST en el mismo callback y, después de procesarlo, devuelve una redirección al frontend. Este proyecto debe conservar además sus decisiones actuales: estado y PKCE de un solo uso, cookies `HttpOnly`, una ruta frontend configurada por ambiente, ausencia de datos sensibles en URL y el backend como autoridad del paso vigente.

## Goals / Non-Goals

**Goals:**

- Aceptar el retorno real de ID Perú v1 por GET y conservar POST para compatibilidad con proveedores o ambientes que lo utilicen.
- Garantizar que toda ejecución del callback termine en una redirección segura al frontend, tanto en éxito como en resultados controlados.
- Resolver la vista posterior desde el estado persistido y el contexto temporal validado por el backend.
- Mostrar el paso 2 mínimo después de una autenticación válida y mantener el paso 1 con un aviso accesible ante fallos.
- Mantener OpenAPI y las pruebas alineados con el comportamiento real del navegador.

**Non-Goals:**

- Implementar el segundo servicio, el listado o la selección funcional de certificados.
- Cambiar OAuth/OIDC, PKCE, cifrado `vd`, validación JWT/JWKS o comparación del DNI salvo una corrección indispensable descubierta por las pruebas.
- Añadir datos del resultado del proveedor a la URL del frontend.
- Crear tablas, sesiones permanentes o recuperación histórica.

## Decisions

### 1. Un único callback compatible con GET y POST

El controlador aceptará GET con parámetros de consulta y POST con formulario en `/api/v1/idperu/callback`. Ambos métodos normalizarán `code`, `state`, `session_state` y `error` y delegarán al mismo caso de uso; no existirán dos flujos de negocio.

Se conserva POST porque la integración v2 o configuraciones futuras pueden usarlo. Se añade GET porque es el retorno comprobado de v1 y del sistema institucional de referencia. No se cambiará el callback registrado ni se introducirá una ruta adicional.

### 2. El callback del navegador siempre responde con redirección

Después de procesar un retorno válido o un error reconocido, el backend responderá `303 See Other` con un `Location` construido desde una URI frontend permitida por configuración. El callback no devolverá al navegador el cuerpo de error estándar de la API.

Los errores de binding, parámetros ausentes, estado inválido o excepción controlada se traducirán dentro de una frontera específica del callback. Cuando el intento pueda identificarse de forma segura se persistirá su estado normalizado. Si no puede asociarse sin debilitar `state`, se utilizará un resultado genérico, breve y protegido por el backend para que la consulta de estado muestre recuperación segura; nunca se confiará en un resultado enviado por query al frontend.

Alternativa descartada: redirigir con `?success`, `?error` o mensajes. Aunque es simple, permite manipular la presentación, contradice la ruta sin estado técnico y deja información del flujo en historial y telemetría del navegador.

### 3. El frontend resuelve el retorno consultando al backend

Al cargar la ruta configurada tras volver de ID Perú, el orquestador consultará el endpoint de verificación vigente con `credentials: include`. La respuesta autorizada determinará una de estas salidas:

- `VERIFIED` y autorización temporal válida: stepper en paso 2 y una presentación mínima de transición, sin certificados ficticios ni controles de selección.
- `CANCELLED`, `REJECTED`, `EXPIRED`, `IDENTITY_MISMATCH`, timeout, indisponibilidad o error controlado: stepper en paso 1 y un único SweetAlert2 con texto ciudadano y una acción permitida.
- Sin contexto válido: regreso limpio a la consulta inicial, sin inferir estado desde la URL.

El aviso se disparará por transición de estado, no por cada render, y su cierre no iniciará solicitudes automáticamente. El reintento reutilizará la acción normal de iniciar una nueva autenticación.

### 4. Rutas por ambiente sin duplicar pantallas

El callback backend conserva la URI registrada `http://localhost:8080/api/v1/idperu/callback` en local. La redirección frontend local puede terminar en `/autorizacion` por compatibilidad con el origen registrado, mientras producción utiliza `/cancelacion`; ambas rutas cargarán el mismo orquestador del flujo y ninguna contendrá parámetros del proveedor. Las rutas no tendrán implementaciones divergentes.

### 5. Cookies y encabezados

Solo el éxito establecerá o rotará la autorización temporal del flujo. Los fallos no emitirán una autorización posidentidad. La redirección mantendrá los atributos de cookie existentes (`HttpOnly`, `SameSite=Lax`, `Secure` según ambiente y expiración corta). `Location` se validará como URI fija configurada y nunca se tomará de la solicitud del navegador.

### 6. Contrato y observabilidad

OpenAPI documentará GET y POST como operaciones de callback que responden por redirección. Los logs conservarán correlación y estado normalizado, pero excluirán query strings, código, state, session_state, tokens, DNI y URL completa de autorización. Las respuestas del callback no incluirán cuerpo con detalles técnicos.

## Risks / Trade-offs

- **[Diferencias entre ID Perú v1 y v2]** → Mantener ambos verbos en una entrada común y probar las formas documentadas sin relajar la validación de campos por versión.
- **[Excepción antes de asociar el state]** → Redirigir a una recuperación genérica y no modificar ningún intento que no pueda identificarse de forma criptográficamente segura.
- **[Alertas repetidas al refrescar]** → Consumir o reconocer una sola vez el resultado de presentación y probar remount, refresh y React Strict Mode.
- **[Paso 2 todavía no funcional]** → Limitarlo a una confirmación visual de transición, sin datos, mocks ni acciones que aparenten una selección implementada.
- **[URI local distinta de la canónica]** → Hacer que `/autorizacion` y `/cancelacion` utilicen el mismo componente y centralizar las constantes de ruta.

## Migration Plan

1. Ampliar el mapping del callback y sus pruebas web sin cambiar la URI registrada.
2. Unificar la respuesta de éxito y error como redirección y validar cookies y ausencia de datos sensibles.
3. Actualizar la resolución del estado en frontend, el paso 2 mínimo y los avisos del paso 1.
4. Actualizar OpenAPI y documentación de prueba local real.
5. Ejecutar pruebas backend, frontend, lint, tipos y una prueba manual v1 desde el navegador.

El rollback consiste en revertir el incremento; no existe migración de datos. Antes del despliegue se debe confirmar que la URI del callback y el origen frontend coinciden exactamente con los registrados en cada ambiente.

## Open Questions

Ninguna para implementar esta corrección. El contenido y comportamiento definitivo del paso 2 pertenecen a la especificación del segundo servicio y selección de certificados.
