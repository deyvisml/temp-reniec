## 1. Preflight y estrategia de migración

- [x] 1.1 Inspeccionar el esquema y los registros del MySQL local para confirmar si contienen únicamente datos ficticios o desechables.
- [x] 1.2 Elegir y documentar la estrategia final: consolidar la línea base y recrear el volumen local si no hay datos relevantes, o crear una migración Flyway hacia adelante si deben conservarse.
- [x] 1.3 Actualizar las migraciones para producir exactamente seis tablas y retirar `cancellation_request_session`, `public_reference`, `consent_version`, `recoverable_until`, `expires_at` y todas las columnas optimistas `version`.

## 2. Simplificación de persistencia backend

- [x] 2.1 Eliminar `CancellationRequestSessionEntity`, `CancellationRequestSessionRepository` y todas sus referencias de producción.
- [x] 2.2 Retirar de `CertificateCancellationRequestEntity` los campos, mapeos, constructor, validaciones y accesores de referencia pública, versión de consentimiento, ventana de recuperación y versión optimista.
- [x] 2.3 Retirar de `RevocationOperationEntity` la versión optimista y adaptar cualquier consulta o prueba dependiente.
- [x] 2.4 Simplificar la confirmación para requerir motivo y `confirmed_at`, sin persistir una versión de consentimiento.
- [x] 2.5 Adaptar los repositorios para recuperar por `id` y para localizar por DNI y estado la solicitud sin finalizar, sin corte temporal, consultas de sesión ni referencia pública.
- [x] 2.6 Revisar las transacciones de inicio y elegibilidad para conservar el control concurrente mediante bloqueos explícitos y sin `@Version`.

## 3. Recuperación simple y contrato API

- [x] 3.1 Ajustar el caso de uso de inicio para recuperar siempre la solicitud sin finalizar directamente por DNI y devolver el estado persistido como progreso.
- [x] 3.2 Cambiar `CancellationRequestResponse` de `publicReference` a `requestId` numérico y eliminar búsquedas por referencia pública.
- [x] 3.3 Mantener la regla de que `requestId` no autentica ni autoriza y que ningún contrato expone el DNI completo.
- [x] 3.4 Actualizar anotaciones OpenAPI, regenerar el documento y regenerar los tipos TypeScript sin `publicReference`.

## 4. Adaptación frontend

- [x] 4.1 Actualizar el cliente de solicitudes y los tipos consumidos para utilizar `requestId`.
- [x] 4.2 Adaptar la transición preparada a `/verificacion-identidad` para transportar únicamente `requestId` y nunca el DNI.
- [x] 4.3 Actualizar las pruebas del formulario, navegación, transporte e integración para el contrato numérico.

## 5. Pruebas del modelo simplificado

- [x] 5.1 Actualizar las pruebas de Flyway para verificar seis tablas y la ausencia de la tabla y columnas eliminadas.
- [x] 5.2 Retirar pruebas de múltiples sesiones y control optimista y sustituirlas por pruebas de recuperación directa y bloqueo concurrente donde corresponda.
- [x] 5.3 Verificar creación, elegibilidad, recuperación indefinida de solicitud sin finalizar, historial terminal e integridad de las seis relaciones restantes en MySQL Testcontainers.
- [x] 5.4 Verificar que solicitudes concurrentes para el mismo DNI no creen duplicados incompatibles sin una columna `version`.
- [x] 5.5 Ejecutar pruebas unitarias y `mvn verify` del backend.
- [x] 5.6 Ejecutar typecheck, pruebas, build, comprobación OpenAPI e integración real del frontend.

## 6. Documentación y cierre

- [x] 6.1 Actualizar `docs/data-model/README.md` con el diagrama de seis tablas, columnas vigentes y recuperación indefinida mediante DNI y estado.
- [x] 6.2 Actualizar `docs/TECHNICAL_DECISIONS.md` para aclarar que la recuperación no implica sesiones persistentes por dispositivo y que JWT se diseñará aparte.
- [x] 6.3 Actualizar los README de backend y frontend y eliminar explicaciones de referencia pública, versión de consentimiento, ventana de recuperación, control optimista y sesiones persistentes.
- [x] 6.4 Documentar el reinicio del volumen Compose cuando se use la línea base consolidada y advertir que no debe aplicarse destructivamente sobre información relevante.
- [x] 6.5 Confirmar mediante búsqueda global que no quedan referencias de producción, contrato, pruebas o documentación a los elementos eliminados fuera del historial OpenSpec archivado.
