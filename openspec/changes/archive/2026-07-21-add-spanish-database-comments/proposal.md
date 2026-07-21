## Why

El esquema MySQL vigente no incluye comentarios en tablas ni columnas, por lo que herramientas como MySQL Workbench o consultas a `INFORMATION_SCHEMA` muestran nombres técnicos sin explicar su propósito. Incorporar descripciones breves en español hará que las ocho tablas y sus campos sean comprensibles directamente desde la base de datos, sin introducir nuevas estructuras ni lógica funcional.

## What Changes

- Añadir comentarios en español a las ocho tablas del modelo consolidado.
- Añadir un comentario claro y breve a cada columna existente, explicando su propósito dentro del dominio o su función técnica.
- Aplicar los comentarios mediante una migración Flyway incremental y reproducible, compatible tanto con una base creada desde cero como con una base que ya ejecutó V1 y V2.
- Verificar mediante `INFORMATION_SCHEMA.TABLES` e `INFORMATION_SCHEMA.COLUMNS` que ninguna tabla o columna del dominio quede sin descripción.
- Documentar una convención sencilla para que toda tabla o columna futura incluya su comentario en español en la misma migración que la crea.
- Mantener intactos nombres, tipos, nulabilidad, valores, claves, índices, restricciones y relaciones existentes.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `cancellation-request-persistence-model`: el esquema persistente deberá ser autodescriptivo mediante comentarios MySQL en español para todas las tablas y columnas del modelo vigente y para las incorporadas posteriormente.

## Impact

- Backend: nueva migración Flyway posterior a V1 y V2 y pruebas de integración del metadato del esquema.
- Base de datos: se modificará únicamente el metadato `COMMENT` de las ocho tablas y sus columnas; no habrá transformación ni pérdida de datos.
- Documentación: actualización breve de `docs/data-model/README.md` con la convención y una consulta de inspección.
- APIs, frontend, entidades JPA y contratos externos: sin cambios.
- Dependencias: no se agregan librerías ni herramientas nuevas.
- Orden: este cambio parte del modelo de ocho tablas de `extend-persistence-for-selectable-certificates` y de la corrección `start-new-request-without-progress-recovery`; ambos deben sincronizarse o archivarse antes de archivar esta propuesta.
