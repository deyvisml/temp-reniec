## Why

El contexto y el índice visual vigentes todavía definen una consulta binaria y un flujo de cuatro pasos que trata todos los certificados como un conjunto. La nueva fuente funcional y la nueva vista de selección establecen un flujo de cinco pasos con emisiones vigentes seleccionables y resultados por certificado, por lo que las referencias permanentes deben actualizarse antes de continuar el desarrollo.

## What Changes

- **BREAKING**: sustituir `docs/context/PROJECT_CONTEXT.md` por el contenido íntegro de `PROJECT_CONTEXT_v2.md`, dejando una sola fuente funcional vigente y sin mantener una segunda copia ambigua.
- Incorporar las seis imágenes adjuntas sin alterar sus binarios y normalizarlas como `home.png`, `step-1.png`, `step-2.png`, `step-3.png`, `step-4.png` y `step-5-final.png`.
- Incorporar la nueva selección de certificados como `step-2.png` y desplazar las referencias anteriores de motivo, confirmación y constancia a los pasos 3, 4 y 5.
- Actualizar el índice visual para documentar el flujo de cinco pasos, la relación entre emisiones y certificados vigentes, las reglas de selección y la precedencia del contexto funcional.
- Registrar explícitamente que las vistas reutilizadas aún contienen steppers, numeraciones o textos del flujo anterior y que solo sirven como referencia de composición visual cuando contradicen el contexto vigente.
- Auditar la documentación técnica y las especificaciones vigentes afectadas; corregir las referencias puramente documentales y marcar claramente las divergencias de la implementación actual que requieren cambios funcionales posteriores.
- Verificar existencia, decodificación, tamaño y SHA-256 de cada archivo incorporado, y comprobar que no permanezcan rutas activas ambiguas como `step-4-final.png`.
- Mantener el cambio estrictamente documental: no modificar backend, frontend, base de datos, mocks, contratos ni comportamiento ejecutable.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `project-reference-materials`: reemplaza la fuente funcional vigente, amplía y renumera las referencias visuales para el flujo de cinco pasos, y establece reglas para tratar inconsistencias y divergencias temporales con la implementación existente.

## Impact

- Afecta `docs/context/PROJECT_CONTEXT.md`, `docs/ui-reference/`, `docs/ui-reference/README.md` y únicamente los documentos o referencias vigentes que describan el flujo anterior.
- Modifica la especificación documental `project-reference-materials` y deja identificados los cambios funcionales futuros necesarios en consulta, persistencia, selección, revocación y constancia.
- No afecta código en `/backend` o `/frontend`, migraciones, entidades JPA, contratos OpenAPI, dependencias ni configuración de ejecución.
- Las imágenes históricas dentro de cambios OpenSpec archivados permanecen como trazabilidad; no se interpretan como referencias vigentes.
