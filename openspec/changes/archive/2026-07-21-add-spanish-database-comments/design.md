## Context

El modelo consolidado se construye mediante Flyway V1 y V2 y contiene ocho tablas de dominio. Sus nombres son razonablemente descriptivos para quien conoce el proyecto, pero MySQL no almacena actualmente una explicación visible para las tablas ni para sus columnas. Los comentarios nativos pueden consultarse desde MySQL Workbench, otros clientes SQL y `INFORMATION_SCHEMA`, lo que facilita explicar y mantener el esquema sin agregar tablas auxiliares.

El cambio depende del esquema efectivo de ocho tablas aportado por `extend-persistence-for-selectable-certificates` y de las reglas de historial de `start-new-request-without-progress-recovery`. Por ello, esas especificaciones deben ser la línea base antes de archivar este incremento.

## Goals / Non-Goals

**Goals:**

- Describir en español las ocho tablas y todas sus columnas directamente en el metadato MySQL.
- Mantener comentarios breves, claros y centrados en el propósito real de cada elemento.
- Aplicar la mejora a bases nuevas y existentes mediante una migración Flyway incremental.
- Verificar automáticamente que no existan tablas o columnas del dominio sin comentario.
- Establecer una convención comprobable para futuros cambios de esquema.

**Non-Goals:**

- Renombrar tablas, columnas, restricciones o índices.
- Cambiar tipos, nulabilidad, valores por defecto, claves, relaciones o datos.
- Duplicar todo el contenido de la documentación funcional dentro de MySQL.
- Crear una tabla diccionario, catálogo de metadatos, procedimiento almacenado o trigger.
- Añadir anotaciones JPA, dependencias, endpoints o cambios de frontend.
- Traducir nombres físicos del esquema al español.

## Decisions

### 1. Usar comentarios nativos de MySQL

Cada tabla recibirá un `TABLE_COMMENT` y cada columna un `COLUMN_COMMENT`. Esta información aparece automáticamente en clientes de base de datos y puede consultarse con `INFORMATION_SCHEMA`.

Alternativa descartada: crear una tabla `data_dictionary`. Agregaría sincronización, consultas y mantenimiento sin mejorar la visibilidad directa de cada campo.

Alternativa descartada: limitar las descripciones a Markdown. El documento seguirá siendo útil, pero no resuelve la necesidad de entender el esquema mientras se inspecciona MySQL.

### 2. Incorporar una migración Flyway V3 hacia adelante

V1 y V2 permanecerán inmutables. V3 aplicará comentarios a las ocho tablas efectivas y a todas sus columnas, de modo que una base existente reciba solo el metadato nuevo y una base vacía termine con el mismo resultado después de ejecutar V1, V2 y V3.

MySQL no ofrece `COMMENT ON COLUMN`; para añadir un comentario es necesario repetir la definición con `ALTER TABLE ... MODIFY COLUMN ... COMMENT`. La migración deberá copiar exactamente tipo, atributos, nulabilidad, valor por defecto, `AUTO_INCREMENT`, charset y collation existentes. Los comentarios de tabla se aplicarán con `ALTER TABLE ... COMMENT`.

Alternativa descartada: editar V1 y V2. Rompería el checksum de instalaciones que ya ejecutaron esas migraciones.

### 3. Mantener comentarios concisos y orientados al propósito

Los comentarios estarán en español, sin abreviaturas ambiguas y sin copiar listas extensas de estados. Explicarán qué representa el dato; cuando corresponda distinguirán identificadores internos, referencias externas, estados controlados, fechas UTC, correlación, idempotencia y control de concurrencia.

Los comentarios no incluirán DNI reales, UUID, credenciales, secretos, payloads ni ejemplos sensibles. Los nombres físicos permanecerán en inglés para evitar un cambio incompatible del esquema.

### 4. Validar cobertura y ausencia de cambios estructurales

Una prueba de integración con MySQL Testcontainers consultará:

- `INFORMATION_SCHEMA.TABLES.TABLE_COMMENT` para las ocho tablas;
- `INFORMATION_SCHEMA.COLUMNS.COLUMN_COMMENT` para todas las columnas de esas tablas;
- metadatos estructurales representativos y validación Hibernate para detectar cambios accidentales de tipo, nulabilidad, valor por defecto o `AUTO_INCREMENT`.

La prueba de migración incremental partirá de V2 y ejecutará V3, demostrando que filas existentes permanecen intactas. También se verificará la construcción desde una base vacía.

### 5. Exigir comentarios en cambios futuros de esquema

`docs/data-model/README.md` indicará que toda migración que cree una tabla o columna debe incorporar su comentario en español en esa misma migración. Las pruebas de cobertura sobre las tablas del dominio se actualizarán cuando el esquema crezca, evitando que el requisito dependa solo de una revisión manual.

## Risks / Trade-offs

- [Repetir la definición de una columna en `MODIFY COLUMN` puede cambiarla accidentalmente] → Copiar literalmente la definición vigente, validar con Hibernate y comprobar metadatos estructurales en Testcontainers.
- [Comentarios demasiado extensos pueden volverse obsoletos] → Describir responsabilidades estables y mantener reglas detalladas en las especificaciones y documentación de dominio.
- [Clientes SQL podrían mostrar los acentos de forma incorrecta] → Mantener el esquema en `utf8mb4` y comprobar desde `INFORMATION_SCHEMA` los textos esperados.
- [V3 depende de las tablas creadas por V2] → Archivar o sincronizar primero el cambio del modelo seleccionable y validar tanto la ruta vacía como la incremental V2→V3.
- [Una futura columna puede quedar sin comentario] → Mantener una consulta de cobertura automatizada y la convención explícita en la documentación.

## Migration Plan

1. Confirmar que V1 y V2 son la línea base aplicada y no modificarlas.
2. Crear V3 con comentarios de tabla y columna para las ocho tablas.
3. Ejecutar Flyway desde una base vacía y validar el esquema con Hibernate.
4. Ejecutar una prueba incremental desde V2 con datos ficticios y confirmar que V3 conserva filas y estructura.
5. Consultar `INFORMATION_SCHEMA` y exigir cobertura completa de comentarios en español.
6. Actualizar la documentación del modelo y ejecutar la verificación completa del backend.

La reversión funcional no es necesaria porque los comentarios no afectan datos ni comportamiento. Si fuera indispensable retirarlos, se haría mediante otra migración hacia adelante; no se editará ni deshará V3 manualmente en ambientes compartidos.

## Open Questions

No existen decisiones funcionales pendientes. El contenido concreto de cada comentario se derivará del modelo y la documentación vigentes durante la implementación.
