## Context

El repositorio conserva `docs/context/PROJECT_CONTEXT.md` y cinco imágenes del flujo original. Cuatro de las seis imágenes entregadas para SPEC-08 coinciden binariamente con referencias actuales, `home.png` y `step-1.png` también permanecen iguales, y la única vista nueva es la selección de certificados. El cambio funcional de contexto es sustancial: la consulta pasa de una elegibilidad booleana a una lista de emisiones vigentes; la selección aparece después de autenticación; la revocación y la constancia manejan resultados por certificado.

La implementación actual todavía responde al modelo anterior. Esta propuesta no puede modificarla ni presentar sus contratos, esquema o estados actuales como si ya cumplieran el contexto v2. Debe establecer una autoridad documental inequívoca y distinguir las referencias funcionales vigentes de la documentación que describe el estado técnico implementado.

## Goals / Non-Goals

**Goals:**

- Incorporar íntegramente el contexto v2 y seis PNG adjuntos en rutas permanentes y no ambiguas.
- Documentar el flujo vigente de cinco pasos y las reglas funcionales esenciales de consulta, privacidad, selección, revocación y constancia.
- Conservar sin edición los diseños reutilizados y registrar sus inconsistencias internas.
- Auditar documentos no archivados para eliminar afirmaciones ambiguas sobre cuál es la fuente funcional vigente.
- Dejar trazables las divergencias entre el contexto v2 y la implementación actual para que cambios posteriores las resuelvan.

**Non-Goals:**

- Modificar código, contratos OpenAPI, dependencias, mocks, entidades, repositorios, migraciones o datos.
- Implementar selección, persistencia de emisiones, revocación múltiple o constancias detalladas.
- Corregir, regenerar o rediseñar el contenido gráfico de los PNG.
- Reescribir cambios OpenSpec archivados, que se conservan como historia.
- Afirmar que el sistema ejecutable ya satisface el contexto v2.

## Decisions

### 1. Una sola fuente funcional vigente

`D:/Downloads/PROJECT_CONTEXT_v2.md` se copiará íntegramente a `docs/context/PROJECT_CONTEXT.md`. No se conservará otra copia activa del contexto anterior. Git y los cambios OpenSpec archivados proporcionan trazabilidad suficiente sin crear dos fuentes de verdad.

Alternativa descartada: conservar `PROJECT_CONTEXT_v1.md` junto al nuevo documento. Aunque facilitaría una comparación inmediata, introduciría ambigüedad sobre qué versión deben consultar las tareas futuras.

### 2. Mapeo explícito por contenido, no por nombre temporal

Los adjuntos se incorporarán según su pantalla observada:

| Destino permanente | Fuente adjunta | Papel vigente |
| --- | --- | --- |
| `home.png` | `codex-clipboard-404ee988-a22b-40e9-b3e4-c1b224c3ad60.png` | Inicio y consulta |
| `step-1.png` | `codex-clipboard-2de24090-3d18-4a8b-a6ff-36e972ed8e9c.png` | Autenticación |
| `step-2.png` | `codex-clipboard-71b8ac03-4259-4fc4-b626-7baf6d82a6f8.png` | Selección de certificados |
| `step-3.png` | `codex-clipboard-73663477-3193-40d3-9f6e-fea589e759f4.png` | Motivo |
| `step-4.png` | `codex-clipboard-39676cde-c969-4e3a-b5ad-981c134e3292.png` | Revisión y confirmación |
| `step-5-final.png` | `codex-clipboard-da91833e-cfae-4048-adea-9bdbefd887fc.png` | Resultado y constancia |

Los destinos actuales `step-2.png`, `step-3.png` y `step-4-final.png` se reorganizarán; `step-4-final.png` se eliminará una vez verificada `step-5-final.png`. No habrá copias activas con nombres del orden anterior.

### 3. Integridad binaria comprobable

Las imágenes se copiarán o moverán sin procesamiento. Se verificará cada destino mediante tamaño, SHA-256 y decodificación PNG contra su adjunto correspondiente. El contexto se verificará por contenido, tamaño y SHA-256. La coincidencia ya observada entre cinco adjuntos y cinco archivos actuales permite renombrar sin pérdida, pero la verificación final se hará sobre los destinos.

### 4. Las inconsistencias visuales se registran, no se corrigen

El README visual incluirá una tabla de inconsistencias. `step-1.png`, `step-3.png`, `step-4.png` y `step-5-final.png` aún muestran el stepper de cuatro pasos; `step-4.png` y `step-5-final.png` además contienen textos que sugieren cancelar todos los certificados. Esos elementos no son reglas vigentes. El contexto v2 prevalece y las imágenes se usan para composición, jerarquía y estilo hasta que cada vista se implemente.

Alternativa descartada: editar los PNG para ajustar numeración y textos. Rompería la integridad de los adjuntos y excedería el alcance documental.

### 5. Auditoría documental sin falsear la implementación

Se revisarán documentos no archivados en `docs/` y especificaciones principales. Las fuentes de orientación funcional se actualizarán al flujo v2. Los documentos cuya finalidad sea describir el código o esquema actualmente implementado conservarán los hechos técnicos, pero llevarán una advertencia clara de divergencia y remitirán al contexto v2; no se reescribirán como si tablas, DTO o endpoints aún inexistentes ya estuvieran disponibles. Las carpetas `openspec/changes/archive/` se consideran historia y no se alteran.

### 6. La fuente visual no crea reglas funcionales

`docs/ui-reference/README.md` establecerá que “emisiones vigentes” es el concepto devuelto por consulta y “certificados digitales vigentes” su presentación ciudadana. También exigirá revisar el contexto y la imagen correspondiente antes de implementar una vista, y resolverá toda contradicción funcional a favor del contexto con registro de pendiente de validación.

## Risks / Trade-offs

- [La implementación seguirá contradiciendo temporalmente el contexto v2] → Documentar la divergencia de forma visible y reservar la adaptación para cambios funcionales posteriores.
- [Los PNG reutilizados pueden inducir a implementar cuatro pasos o cancelación total] → Mantener una tabla explícita de inconsistencias y precedencia en el README visual.
- [Renombrar archivos puede romper enlaces] → Buscar referencias no archivadas antes y después, actualizar enlaces afectados y verificar que no quede `step-4-final.png` como ruta vigente.
- [Una búsqueda textual puede detectar historia legítima] → Excluir `openspec/changes/archive/` de la validación de referencias vigentes; la historia permanece inmutable.
- [El archivo de contexto es grande y puede sufrir cambios de codificación] → Copiarlo sin transformación y comparar tamaño y SHA-256 con el adjunto.

## Migration Plan

1. Registrar hashes, tamaños y dimensiones de los siete adjuntos y de los destinos actuales.
2. Sustituir el contexto vigente y reorganizar/copiar los PNG según la tabla de mapeo.
3. Actualizar el README visual y las referencias documentales no archivadas afectadas.
4. Añadir advertencias de divergencia a documentos técnicos que describen fielmente la implementación anterior y no pueden reescribirse sin cambiar código.
5. Verificar integridad de archivos, enlaces Markdown, conjunto exacto de nombres y ausencia de referencias activas ambiguas.
6. Confirmar mediante `git diff` que no existe ningún cambio bajo `/backend` o `/frontend`.

Rollback: restaurar los documentos y nombres anteriores desde Git. No existe migración de datos ni despliegue ejecutable.

## Open Questions

- Los contratos institucionales de consulta y revocación siguen pendientes; el contexto define únicamente el comportamiento propio esperado y no un DTO externo definitivo.
- La estrategia de persistencia de la lista de emisiones, los resultados por UUID y las constancias parciales deberá diseñarse en un cambio funcional posterior.

