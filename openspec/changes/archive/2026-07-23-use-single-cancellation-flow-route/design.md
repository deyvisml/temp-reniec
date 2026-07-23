## Context

El frontend expone actualmente la página inicial en `/`, el paso de autenticación en `/verificacion-identidad` y el procesamiento del retorno en `/verificacion-identidad/retorno`. Las tres vistas forman parte de un único trámite ciudadano y la incorporación de los pasos restantes multiplicaría rutas que describen detalles internos del flujo.

La aplicación ya utiliza una solicitud persistida en el backend y una autorización temporal mediante cookie `HttpOnly` para proteger la continuidad de la autenticación. No existe una sesión permanente ni se permite recuperar solicitudes finalizadas. La integración real con ID Perú abandona temporalmente el sitio y retorna primero al callback del backend, que finalmente redirige al frontend.

El cambio atraviesa la estructura App Router del frontend, la navegación iniciada desde la consulta de disponibilidad, el destino posterior al callback de ID Perú, la configuración local y la documentación. No cambia el protocolo OAuth/OIDC, los endpoints funcionales ni el modelo de datos.

## Goals / Non-Goals

**Goals:**

- Utilizar `/cancelacion` como única URL visible y canónica durante todo el flujo ciudadano.
- Resolver dentro de esa ruta la vista correspondiente al estado vigente del trámite.
- Mantener fuera de la URL el DNI, el identificador de solicitud, certificados, resultados, tokens y nombres de pasos.
- Retornar desde ID Perú a la misma ruta y continuar de forma segura mediante el estado validado por el backend.
- Centralizar las rutas del flujo y retirar la navegación funcional hacia páginas específicas por paso.
- Mantener compatibilidad transitoria mediante redirecciones desde `/` y las rutas antiguas.

**Non-Goals:**

- Implementar los pasos funcionales posteriores a la autenticación.
- Crear recuperación histórica, sesión permanente o restauración de solicitudes finalizadas.
- Introducir parámetros de consulta para seleccionar el paso o transportar resultados.
- Incorporar un gestor global de estado o cambiar el modelo de persistencia.
- Modificar el flujo OAuth/OIDC, PKCE, `state` o la validación de tokens de ID Perú.

## Decisions

### 1. La ruta canónica será `/cancelacion`

Se utilizará el término ciudadano que representa el trámite completo. `/revocacion` se descarta porque la revocación es una operación técnica posterior y no el nombre del flujo completo. Una ruta genérica como `/tramite` sería menos clara y una ruta extensa agregaría complejidad sin aportar contexto útil.

`/` redirigirá a `/cancelacion` y las rutas existentes `/verificacion-identidad` y `/verificacion-identidad/retorno` mantendrán únicamente redirecciones de compatibilidad hacia la ruta canónica. No conservarán implementaciones duplicadas.

### 2. Un coordinador del flujo renderizará la vista activa

La página App Router de `/cancelacion` compondrá un coordinador de flujo que mostrará la consulta inicial, autenticación o las vistas que se incorporen posteriormente. El coordinador utilizará el resultado inmediato de las acciones y, cuando corresponda, el contexto temporal consultado al backend mediante la cookie segura existente.

Se descarta codificar el paso en el path, un parámetro `?step=` o el fragmento de URL porque expondría estado manipulable, permitiría saltos inválidos y mantendría la fragmentación que este cambio corrige.

### 3. La URL no será la fuente de verdad del progreso

La fuente de verdad seguirá siendo el estado controlado por el backend. El frontend podrá conservar estado efímero de presentación, pero cada vista protegida deberá validarse contra el contexto activo. Un acceso directo a `/cancelacion` sin contexto vigente mostrará el inicio. Un trámite finalizado o expirado no restaurará la última pantalla.

Esta continuidad temporal necesaria para completar el retorno OAuth no se interpretará como recuperación histórica o multidispositivo.

### 4. El retorno de ID Perú finalizará en `/cancelacion`

El `redirect_uri` OAuth continuará apuntando al callback del backend. Después de validar `state`, intercambiar el código y crear la autorización temporal, el backend redirigirá al navegador a la URL frontend configurada `/cancelacion`. La página consultará el estado vigente y mostrará el resultado apropiado.

No se agregarán a la URL indicadores como `success`, `error`, `requestId`, códigos, tokens o datos personales. Los errores funcionales se resolverán con el contrato existente y el contexto temporal.

### 5. Las rutas y transiciones se centralizarán

Una definición única en el frontend contendrá la ruta canónica. Los componentes no repetirán literales ni usarán `window.location.assign` para cambiar entre pasos internos. Las navegaciones que solo canonicalicen una URL utilizarán redirección o reemplazo para no poblar el historial con rutas obsoletas.

La salida hacia ID Perú sí conservará una navegación completa porque abandona la aplicación; el retorno seguirá terminando en `/cancelacion`.

## Risks / Trade-offs

- [Una recarga puede perder estado efímero del componente] → Resolver las vistas protegidas mediante el contexto temporal del backend; sin contexto válido se vuelve al inicio de forma controlada.
- [Las rutas antiguas pueden permanecer en marcadores o documentación externa] → Mantener redirecciones transitorias y actualizar todas las referencias internas y ejemplos de configuración.
- [Un coordinador único puede crecer con los cinco pasos] → Separar cada vista por funcionalidad y mantener en el coordinador únicamente selección de estado y transiciones.
- [La configuración de retorno de ID Perú puede quedar desalineada entre ambientes] → Centralizar la variable, actualizar `.env.example`, pruebas y documentación, y validar el destino exacto por ambiente.
- [El historial del navegador puede incluir la salida al proveedor externo] → Aceptar el historial propio del flujo OAuth, pero evitar entradas adicionales por cambios internos de paso.

## Migration Plan

1. Crear la constante de ruta `/cancelacion` y la página coordinadora sin retirar todavía las rutas existentes.
2. Mover la composición de inicio y autenticación a la ruta canónica y adaptar sus transiciones internas.
3. Cambiar el destino frontend posterior al callback de ID Perú a `/cancelacion` en configuración, ejemplos y pruebas.
4. Convertir `/`, `/verificacion-identidad` y `/verificacion-identidad/retorno` en redirecciones hacia la ruta canónica.
5. Actualizar documentación y ejecutar pruebas unitarias, integración y navegación real.
6. En caso de reversión, restaurar las páginas anteriores y el valor previo de la URL frontend de retorno; no existe migración de datos que revertir.

## Open Questions

No quedan decisiones funcionales abiertas para esta propuesta. La forma concreta de resolver las vistas de los pasos 2 a 5 se incorporará con sus respectivas especificaciones, conservando `/cancelacion` como ruta canónica.
