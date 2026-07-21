## Why

El modelo consolidado conserva la solicitud ciudadana y sus operaciones técnicas, pero solo registra una elegibilidad global y un resultado global de revocación. El contexto v2 exige conservar cada emisión vigente consultada, la selección expresa del ciudadano y el resultado individual por certificado, por lo que la persistencia debe evolucionar antes de implementar los siguientes pasos del flujo.

## What Changes

- Mantener `certificate_cancellation_request` como raíz del trámite y conservar las seis tablas existentes sin rediseñar ni eliminar información correctamente modelada.
- Añadir mediante una migración Flyway `V2` una tabla `cancellation_request_certificate` para cada certificado obtenido por una consulta, relacionada con la solicitud y con el intento de elegibilidad que lo originó.
- Persistir número de orden, fecha de creación de la emisión, UUID canónico, estado de disponibilidad, fecha de consulta, selección, fecha de selección, versión optimista y timestamps técnicos.
- Evitar duplicados con unicidad por solicitud y UUID, y garantizar que el intento de consulta pertenezca a la misma solicitud.
- Guardar la selección en la propia fila del certificado, sin crear una tabla exclusiva de selección.
- Añadir una tabla `certificate_revocation_result` para el resultado individual de cada certificado incluido en una operación de revocación.
- Garantizar mediante claves foráneas compuestas que la operación, el certificado y el UUID enviado pertenezcan a la misma solicitud; impedir duplicados por operación y certificado.
- Ampliar los enums de solicitud, disponibilidad, resultado individual y resultado general para representar ausencia, disponibilidad, selección, revocación exitosa, parcial, fallida o incierta sin eliminar valores existentes necesarios para compatibilidad.
- Incorporar repositorios y consultas mínimas para recuperar certificados consultados o seleccionados, resultados de una operación y agregados necesarios para derivar el resultado general.
- Añadir control de concurrencia únicamente en los nuevos registros mutables mediante `@Version`, manteniendo bloqueos explícitos donde el modelo actual ya los requiere.
- Actualizar las pruebas MySQL/Testcontainers para validar una base limpia y la actualización incremental de una base con V1 y datos existentes.
- Actualizar el diagrama, las restricciones, los estados y las consultas de inspección en la documentación del modelo.
- Mantener fuera de alcance endpoints, cambios de frontend, consumo del servicio de consulta, autenticación, selección visual, revocación real o mock y generación de constancias.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `cancellation-request-persistence-model`: amplía el modelo persistente con certificados asociados a la solicitud, selección sobre esos certificados, resultados individuales de revocación, integridad entre solicitudes y cálculo del resultado general.
- `backend-foundation`: permite la evolución incremental V2, las dos entidades persistentes justificadas y sus pruebas de migración sin ampliar el alcance funcional del backend.

## Impact

- Backend: nuevas entidades, enums y repositorios en el módulo de persistencia; ampliaciones acotadas de los estados existentes y pruebas de integración.
- Base de datos: nueva migración incremental `V2`, dos tablas, índices, restricciones y claves foráneas; `V1` permanece inmutable y una instalación limpia ejecuta V1 seguida de V2.
- Datos existentes: se conservan; la migración no exige recrear la base ni inventa certificados para solicitudes históricas.
- Seguridad: el UUID se almacena una vez en formato canónico legible, coherente con el modelo transparente acordado, y se protege evitando exposición previa a autenticación, logs, URLs y respuestas no autorizadas; no se añaden columnas `_cipher` o hashes duplicados.
- Frontend y API: sin cambios en esta tarea.
