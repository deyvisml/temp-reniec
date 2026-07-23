## Context

La portada ya valida el DNI y el backend normaliza respuestas funcionales, pero el lenguaje `eligibility`, varios estados, el contexto funcional y la relación `cancellation_request_certificate.eligibility_check_id` todavía provienen de la regla sustituida: la consulta inicial retornaba la lista detallada. La regla confirmada establece dos integraciones temporal y funcionalmente distintas. Antes de autenticarse solo se confirma existencia; después de autenticarse otro servicio obtendrá los certificados detallados.

La corrección atraviesa documentación, API, frontend, persistencia, estados, mocks y pruebas. Debe preservar solicitudes e intentos existentes mediante Flyway, conservar la tabla de certificados para el paso 2 y evitar crear ahora infraestructura vacía para un contrato externo todavía desconocido.

## Goals / Non-Goals

**Goals:**

- Hacer explícita y verificable la frontera entre consulta de existencia y listado detallado.
- Garantizar que la operación inicial nunca construya, devuelva o persista certificados individuales.
- Distinguir ausencia confirmada de errores, timeout, indisponibilidad o resultado inconcluso.
- Usar nombres de contrato, estados y persistencia que permitan explicar claramente qué información existe en cada etapa.
- Preservar los datos actuales mediante una migración incremental y mantener `cancellation_request_certificate` para el futuro paso 2.
- Mantener sincronizados OpenAPI, tipos TypeScript, comportamiento ciudadano y pruebas.

**Non-Goals:**

- Implementar ID Perú, el segundo servicio, su contrato, su mock o sus intentos persistentes.
- Obtener o persistir la lista real, mostrar selección o crear rutas del paso 2.
- Modificar selección, motivo, confirmación, revocación atómica o constancia.
- Agregar reCAPTCHA, JWT, recuperación de trámites, dependencias o infraestructura productiva.

## Decisions

### 1. Dos puertos funcionales, pero solo el primero se implementa ahora

El puerto actual se renombrará conceptualmente a `CertificateAvailabilityGateway`. Recibirá el DNI y devolverá un resultado interno normalizado sin colecciones: `AVAILABLE`, `NOT_AVAILABLE`, `INCONCLUSIVE`, `UNAVAILABLE` o `ERROR`, además de referencia externa y código técnico opcionales. El adaptador institucional futuro mapeará `true` a `AVAILABLE` y `false` a `NOT_AVAILABLE`; excepciones y respuestas inválidas nunca se mapearán a `NOT_AVAILABLE`.

El segundo puerto de listado se documentará como responsabilidad futura del paso 2, pero no se creará una interfaz vacía ni un DTO especulativo hasta conocer la tarea y el contrato correspondiente.

**Alternativa descartada:** conservar `CertificateEligibilityGateway` y explicar el booleano solo en documentación. Mantendría la ambigüedad entre “es elegible” y “ya se obtuvieron certificados”.

### 2. La ruta de inicio se conserva y su representación se hace explícita

Se mantendrá `POST /api/v1/cancellation-requests` porque sigue creando la solicitud e iniciando el flujo. El cuerpo con DNI y la regla de validación no cambian. El DTO responderá `availabilityResult` con valores explícitos y conservará `requestId`, `maskedDni`, `requestStatus`, `canContinue` y `nextStep`.

Este cambio del nombre y valores del campo es incompatible para clientes compilados contra el snapshot anterior, por lo que OpenAPI, `frontend/openapi/backend-api.json`, tipos generados y cliente se actualizarán juntos. La versión de ruta no cambia porque el proyecto aún está en desarrollo y ambos lados se despliegan coordinadamente.

**Alternativa descartada:** añadir el campo nuevo y conservar `eligibilityResult` como alias. Duplicaría la misma decisión y prolongaría un contrato que se desea retirar.

### 3. Los estados separan existencia, autenticación y listado

La consulta positiva terminará en `PENDING_IDENTITY_VERIFICATION`; la negativa en `NO_CERTIFICATES_AVAILABLE`; y los resultados inconclusos o fallidos permanecerán bloqueados en un estado reintentable que no implica ausencia. Se añadirá `AUTHENTICATED_PENDING_CERTIFICATE_LIST` para el punto posterior a autenticación y se reservará `CERTIFICATES_AVAILABLE` exclusivamente para una lista detallada no vacía ya persistida. `CERTIFICATES_SELECTED` seguirá representando una selección posterior.

Los nombres heredados `ELIGIBLE`, `NOT_ELIGIBLE` y el uso prematuro de `CERTIFICATES_AVAILABLE` se eliminarán del flujo actual y sus datos de desarrollo se normalizarán mediante migración cuando corresponda.

### 4. La persistencia inicial se renombra y los certificados se desacoplan

Una migración Flyway posterior a V4 realizará, preservando filas:

- `certificate_eligibility_check` → `certificate_availability_check`.
- `certificate_cancellation_request.eligibility_result` → `availability_result` y conversión controlada de valores heredados.
- Renombrado coherente de restricciones, índices, entidades, repositorios y comentarios en español cuando MySQL lo permita sin reconstrucción destructiva.
- Eliminación de la clave foránea, índice y columna `cancellation_request_certificate.eligibility_check_id`, porque el primer intento nunca es fuente de certificados.

La tabla `cancellation_request_certificate` y sus datos de prueba representativos se conservarán. No se añadirá todavía `certificate_listing_check`: cuando el segundo servicio se implemente, su tarea decidirá si el contrato real justifica una entidad de intentos y añadirá entonces la relación correspondiente. Hasta ese momento no habrá escritura funcional de certificados.

**Alternativa descartada:** dejar `eligibility_check_id` nullable. Un campo con nombre y semántica incorrectos seguiría sugiriendo que la consulta inicial obtuvo el certificado.

### 5. El mock prueba estados del servicio, no datos del dominio detallado

Los DNI ficticios documentados continuarán produciendo determinísticamente positivo, negativo, inconcluso, indisponible, timeout y error técnico. El mock devolverá únicamente el resultado de disponibilidad. El timeout se verificará por el límite del caso de uso y no se convertirá en un negativo funcional.

### 6. La ausencia de certificados individuales se convierte en una invariante probada

Además de pruebas de resultado y transición, las pruebas de integración contarán cero filas en `cancellation_request_certificate` después de cada consulta inicial, inspeccionarán el JSON/OpenAPI para impedir colecciones o campos de certificado y verificarán que logs, URLs y almacenamiento del navegador no contienen DNI completo ni UUID. Las pruebas persistentes de la entidad de certificados continuarán como preparación aislada del paso 2, sin pasar por el primer gateway.

### 7. La inconsistencia entre ambas consultas queda reservada al paso 2

El contexto y las especificaciones establecerán que un `AVAILABLE` inicial no garantiza que el listado posterior sea no vacío. Si después de autenticación el segundo servicio devuelve cero certificados, la solicitud deberá bloquearse con `NO_CERTIFICATES_AVAILABLE`, sin atribuirlo a autenticación ni inventar registros. Este cambio solo documenta el comportamiento; su implementación pertenecerá a la tarea del paso 2.

## Risks / Trade-offs

- **[Cambio incompatible del DTO]** → regenerar y verificar OpenAPI/TypeScript en el mismo incremento; no mantener dos campos equivalentes.
- **[Migración de nombres sobre datos existentes]** → probar V1→V5 y V4→V5 con filas representativas, validar comentarios, claves e Hibernate y no editar migraciones aplicadas.
- **[Certificados existentes vinculados al intento antiguo]** → la migración elimina únicamente la relación de procedencia y conserva la solicitud, UUID, orden, fechas y selección; los datos históricos no se borran.
- **[Tabla de certificados temporalmente sin entidad de procedencia]** → aceptar el desacoplamiento explícito hasta implementar el segundo servicio, evitando una tabla o contrato inventado.
- **[Estados heredados en entornos de desarrollo]** → transformar solo equivalencias inequívocas y conservar valores históricos que pertenezcan a etapas posteriores.
- **[Enumeración de ciudadanos]** → mantener mensajes genéricos, DNI enmascarado, ausencia de cantidades y los controles existentes; la política definitiva de rate limiting permanece fuera de alcance.

## Migration Plan

1. Corregir primero contexto y especificaciones para fijar la terminología objetivo.
2. Añadir la migración Flyway incremental con renombres, conversión de valores, comentarios y desacoplamiento del certificado.
3. Actualizar entidades, repositorios y pruebas de migración hasta que Hibernate valide tanto base limpia como actualización desde V4.
4. Renombrar el puerto, resultados, caso de uso, DTO y mock; adaptar manejo de errores y transiciones.
5. Actualizar OpenAPI, snapshot, tipos generados y frontend en un mismo cambio coordinado.
6. Ejecutar pruebas rápidas, Testcontainers, contrato, TypeScript y compilaciones completas.

Para reversión durante desarrollo se restaurará el código anterior y se recreará únicamente la base local desechable. No se diseñará una migración destructiva inversa para ambientes con información relevante; una reversión real requerirá una migración hacia adelante específica.

## Open Questions

- El nombre, transporte y contrato institucional del segundo servicio siguen pendientes y no condicionan el contrato interno de existencia.
- La política definitiva de reintentos después de autenticación y lista vacía se decidirá al implementar el paso 2.
- La exposición exacta del identificador UUID en la interfaz permanece sujeta a validación institucional.
