## 1. Referencias y regla vigente

- [x] 1.1 Incorporar la imagen SPEC-16A suministrada como referencia permanente de `docs/ui-reference/step-2.png`, sin regenerarla, y verificar que sea un PNG legible.
- [x] 1.2 Actualizar `docs/context/PROJECT_CONTEXT.md`, `docs/TECHNICAL_DECISIONS.md` y `docs/ui-reference/README.md` para establecer una solicitud, un certificado seleccionado y una revocación futura por operación.
- [x] 1.3 Documentar que las selecciones múltiples y los textos plurales visibles en la imagen son inconsistencias no vigentes y que solo su composición visual sirve de referencia.

## 2. Integridad de persistencia

- [x] 2.1 Crear la siguiente migración Flyway incremental con un índice único funcional que permita como máximo una fila `selected=true` por `request_id`, sin agregar tabla, trigger ni segunda referencia de selección.
- [x] 2.2 Mantener `selected`, `selected_at` y `version` en las filas existentes y ajustar consultas o documentación del repositorio para obtener cero o un certificado seleccionado.
- [x] 2.3 Definir y documentar el fallo seguro de la migración ante datos previos con varias selecciones, sin escoger ni borrar silenciosamente evidencia ciudadana.
- [x] 2.4 Actualizar el diagrama y la documentación del modelo para describir la lista completa consultada y una sola fila seleccionada.

## 3. Contrato y lógica del backend

- [x] 3.1 Cambiar el DTO de selección de `certificateUuids` a un `certificateUuid` canónico, obligatorio y singular, rechazando el contrato anterior y valores no válidos.
- [x] 3.2 Ajustar el caso de uso para validar sesión, estado, pertenencia y disponibilidad del certificado singular antes de modificar filas.
- [x] 3.3 Reemplazar la selección anterior dentro de una sola transacción bloqueada, conservar idempotencia para el mismo UUID y traducir conflictos concurrentes de manera controlada.
- [x] 3.4 Mantener los certificados no elegidos en la solicitud como filas disponibles y evitar que la selección altere el resultado del segundo servicio.
- [x] 3.5 Ajustar auditoría y respuestas para registrar una selección singular sin contadores o conjuntos ambiguos.

## 4. Paso 2 en el frontend

- [x] 4.1 Sustituir el estado `Set`, las casillas y “seleccionar todos” por un UUID opcional y un grupo accesible de botones de radio.
- [x] 4.2 Adaptar las tarjetas a la nueva referencia visual manteniendo una sola tarjeta activa, foco visible, navegación por teclado y comportamiento responsive.
- [x] 4.3 Corregir títulos, ayudas, estado de selección y acciones a redacción singular; no seleccionar automáticamente una lista de un elemento.
- [x] 4.4 Habilitar Continuar únicamente con una selección y prevenir doble envío mientras el reemplazo se procesa.
- [x] 4.5 Conservar los flujos existentes de lista vacía, sesión expirada, timeout, indisponibilidad, respuesta inválida y regreso permitido.

## 5. Revisión y confirmación singular

- [x] 5.1 Cambiar el DTO de revisión de `certificates` a un objeto `certificate` singular construido exclusivamente desde persistencia.
- [x] 5.2 Exigir exactamente una fila seleccionada, disponible y perteneciente a la solicitud tanto al cargar el resumen como al confirmar.
- [x] 5.3 Actualizar el paso 4 para mostrar un solo certificado, usar textos singulares y reconstruir el resumen cuando la selección se reemplace antes de confirmar.
- [x] 5.4 Mantener consentimiento, idempotencia, inmutabilidad posterior y ausencia de llamadas de revocación o generación de constancia.

## 6. Contratos, pruebas y verificación

- [x] 6.1 Actualizar anotaciones OpenAPI, snapshot y tipos TypeScript para `certificateUuid` y `certificate`, y ejecutar la comprobación de sincronización.
- [x] 6.2 Actualizar pruebas unitarias del backend para selección singular, reemplazo idempotente, UUID ajeno, carga antigua con lista y confirmación con cero o varias filas.
- [x] 6.3 Actualizar pruebas MySQL/Testcontainers para el índice condicionado, migración limpia e incremental, concurrencia y rechazo de dos selecciones activas.
- [x] 6.4 Actualizar pruebas del frontend para radio group, elección explícita con un elemento, reemplazo con varios, textos singulares, doble envío y resumen del paso 4.
- [x] 6.5 Ejecutar pruebas, compilación, verificación de tipos, OpenAPI, Flyway/Hibernate y validación OpenSpec estricta.
- [x] 6.6 Revisar el diff final para confirmar que no se implementaron revocación, constancia, nuevas tablas ni dependencias innecesarias.
