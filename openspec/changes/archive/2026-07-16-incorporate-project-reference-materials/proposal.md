## Why

El contexto funcional y los diseños UI existen únicamente como archivos adjuntos externos, por lo que las tareas futuras no cuentan con fuentes permanentes y versionadas dentro del repositorio. Incorporarlos como documentación de referencia establece una base compartida antes de iniciar cualquier implementación funcional.

## What Changes

- Incorporar una copia íntegra de `PROJECT_CONTEXT.md` en `docs/context/PROJECT_CONTEXT.md`.
- Incorporar las cinco imágenes UI originales en `docs/ui-reference/`, normalizando únicamente `step-4 (final).png` como `step-4-final.png`.
- Crear `docs/ui-reference/README.md` para mapear cada imagen con la vista del flujo que representa y establecer reglas claras para el uso conjunto del contexto funcional y los diseños.
- Crear un documento breve dentro de `docs` que registre las decisiones técnicas acordadas para backend, frontend, base de datos, autenticación, persistencia, integraciones y límites arquitectónicos.
- Verificar que los archivos incorporados conserven su contenido original y que todas las imágenes puedan abrirse desde sus rutas permanentes.
- Mantener fuera de este cambio toda creación, configuración o implementación de aplicaciones, infraestructura, datos, APIs, integraciones y funcionalidades del flujo ciudadano.

## Capabilities

### New Capabilities

- `project-reference-materials`: Define la disponibilidad, organización, integridad y reglas de uso de las fuentes permanentes de contexto funcional, referencia visual y decisiones técnicas del proyecto.

### Modified Capabilities

Ninguna.

## Impact

- Se añadirán únicamente archivos documentales y binarios bajo `docs/`.
- No se afectarán código ejecutable, APIs, dependencias, bases de datos, servicios externos ni procesos de despliegue.
- Las tareas posteriores de dominio o interfaz deberán consultar estas fuentes antes de implementar cambios.
