## 1. Persistencia e invariantes de confirmación

- [x] 1.1 Crear la siguiente migración Flyway incremental para añadir `consent_version` nullable, con longitud acotada y comentario en español, conservando los registros históricos y la validación Hibernate.
- [x] 1.2 Mapear la versión de consentimiento en la entidad de solicitud y reforzar que motivo, descripción, selección, fecha y versión no puedan cambiar después de confirmar.
- [x] 1.3 Incorporar una consulta de repositorio con bloqueo de la solicitud actual para serializar confirmaciones concurrentes sin agregar una columna genérica de versión.
- [x] 1.4 Agregar pruebas de migración y persistencia para registros históricos, nuevas confirmaciones completas, inmutabilidad y ausencia de operaciones de revocación.

## 2. Caso de uso y API del paso 4

- [x] 2.1 Implementar el catálogo backend del texto de consentimiento y su versión estable, sin variables de ambiente ni duplicación del texto en la base de datos.
- [x] 2.2 Crear DTO y servicio de resumen que resuelvan la solicitud desde la sesión y devuelvan DNI y UUID enmascarados, certificados seleccionados, motivo, consecuencias y consentimiento desde persistencia.
- [x] 2.3 Implementar la confirmación transaccional con revalidación de sesión, identidad, estado, motivo, pertenencia/disponibilidad de selección y versión aceptada.
- [x] 2.4 Hacer idempotente el doble envío, registrar una sola fecha y un solo evento `CONSENT_CONFIRMED`, y devolver conflictos controlados para estados o concurrencias incompatibles.
- [x] 2.5 Exponer las operaciones protegidas de resumen y confirmación bajo el recurso `current`, con errores uniformes, correlación y sin aceptar DNI, solicitud, motivo o certificados como fuente de verdad del cliente.
- [x] 2.6 Actualizar la resolución de estado de la sesión para permitir el paso 4 y conservar `CONFIRMED` como estado preparado, sin crear ni ejecutar revocación.

## 3. Contrato y documentación de API

- [x] 3.1 Documentar en OpenAPI las operaciones, DTO, validaciones, respuestas exitosas, `401`, `409` y errores controlados del resumen y la confirmación.
- [x] 3.2 Sincronizar el snapshot OpenAPI y los tipos TypeScript desde un backend actualizado y comprobar que no se exponen DNI o UUID completos en el resumen.

## 4. Interfaz ciudadana y navegación

- [x] 4.1 Crear el cliente HTTP tipado del paso 4 usando el cliente centralizado, cookies y renovación de sesión existentes.
- [x] 4.2 Implementar la vista responsive y accesible basada en `step-4.png`, corregida a paso 4 de 5 y al conjunto seleccionado, con resumen, consecuencias y consentimiento explícito.
- [x] 4.3 Integrar estados de carga, error, conflicto por versión, sesión expirada, confirmación en curso y resultado confirmado sin mostrar detalles internos.
- [x] 4.4 Integrar el paso 4 en el resolutor de la ruta interna y el stepper, permitiendo volver solo al paso 3 antes de confirmar y bloqueando identidad, paso 5 y navegación durante el envío.
- [x] 4.5 Asegurar que el frontend no mantenga una copia autoritativa del resumen, no persista datos personales y no invoque todavía revocación ni constancia.

## 5. Pruebas y verificación integral

- [x] 5.1 Agregar pruebas unitarias del servicio para resumen normal/OTHER, consentimiento obligatorio y obsoleto, estados inválidos, selección manipulada, doble confirmación y concurrencia.
- [x] 5.2 Agregar pruebas HTTP con sesión válida, ausente y expirada, contratos minimizados, correlación y errores uniformes.
- [x] 5.3 Agregar pruebas de componentes para renderizado del resumen, checkbox obligatorio, bloqueo durante envío, regreso al paso 3, stepper permitido y comportamiento responsive/accesible.
- [x] 5.4 Agregar una prueba integrada frontend-backend-MySQL que confirme una solicitud una sola vez y demuestre que no existe operación de revocación ni constancia.
- [x] 5.5 Actualizar README y decisiones técnicas con el punto de no retorno, la versión de consentimiento y la separación entre confirmación y revocación.
- [x] 5.6 Ejecutar `mvnw verify`, pruebas frontend, typecheck, build, verificación OpenAPI, integración y `git diff --check`, corrigiendo cualquier regresión antes de completar el cambio.
