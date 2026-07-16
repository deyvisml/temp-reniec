## Context

El repositorio contiene la configuración inicial de OpenSpec, pero todavía no contiene aplicaciones ni documentación permanente del dominio o de las interfaces. El documento funcional y las cinco imágenes UI se encuentran fuera del repositorio como adjuntos y deben incorporarse sin alterar su contenido. Este cambio prepara las fuentes de referencia para quienes realicen tareas posteriores, sin iniciar la solución funcional.

## Goals / Non-Goals

**Goals:**

- Establecer rutas permanentes, sencillas y predecibles para el contexto funcional y los diseños UI.
- Conservar exactamente los bytes de los seis archivos fuente, permitiendo solo normalizar el nombre del archivo final.
- Documentar la relación de cada imagen con el flujo y las reglas de precedencia entre contexto y diseño.
- Registrar de forma breve las decisiones técnicas ya acordadas, sin convertirlas todavía en configuración o código.
- Hacer verificable que cada archivo fue incorporado correctamente y que cada PNG es legible.

**Non-Goals:**

- Crear `/backend` o `/frontend`, configurar tecnologías o instalar dependencias.
- Implementar vistas, endpoints, JWT, persistencia, esquemas, migraciones, mocks, integraciones, constancias o cualquier parte del flujo ciudadano.
- Definir el modelo de datos definitivo o contratos externos no confirmados.
- Modificar, regenerar, optimizar o rediseñar las imágenes.
- Incorporar módulos administrativos o funciones ajenas al flujo ciudadano.

## Decisions

### Organización documental

Se usarán `docs/context/PROJECT_CONTEXT.md` para la fuente funcional, `docs/ui-reference/` para las imágenes y su índice, y `docs/TECHNICAL_DECISIONS.md` para el resumen arquitectónico. Esta separación permite localizar rápidamente cada tipo de referencia sin duplicar el documento de contexto. Como alternativa se consideró reunir todo en un único directorio o documento, pero dificultaría distinguir fuentes funcionales, visuales y decisiones técnicas.

### Preservación de los adjuntos

Los seis adjuntos se copiarán como archivos binarios o textuales sin transformación. `step-4 (final).png` será copiado como `step-4-final.png`; el cambio de nombre no altera su contenido. La integridad se comprobará comparando tamaño y SHA-256 entre origen y destino, y las imágenes se validarán además mediante lectura de su formato. No se usarán herramientas de edición, conversión, compresión ni regeneración.

### Índice y reglas de autoridad

`docs/ui-reference/README.md` enlazará las cinco imágenes y describirá la vista correspondiente. También declarará que `PROJECT_CONTEXT.md` es la fuente funcional principal, que las imágenes son la referencia visual principal y que no autorizan a inventar reglas. Ante contradicciones funcionales prevalecerá el contexto y la diferencia se registrará como pendiente de validación. Todas las tareas posteriores de dominio o UI deberán revisar ambas fuentes.

### Registro técnico acotado

`docs/TECHNICAL_DECISIONS.md` registrará el stack y los límites acordados: Spring Boot, Next.js, MySQL, Tailwind CSS, carpetas futuras `/backend` y `/frontend`, JWT, persistencia y recuperación del progreso, arquitectura incremental, modelo de datos sin sobreingeniería, exclusión de patrones distribuidos innecesarios, integraciones por interfaces con mocks reemplazables y ausencia de módulos administrativos. El documento no contendrá configuraciones, contratos, modelos definitivos ni decisiones funcionales nuevas.

## Risks / Trade-offs

- [Los archivos copiados podrían alterarse accidentalmente] → Comparar tamaño y SHA-256 de cada origen y destino después de la copia.
- [Una imagen podría conservar el nombre pero no ser legible] → Abrir o decodificar cada PNG desde su ruta final durante la verificación.
- [El resumen técnico podría duplicar o divergir del contexto] → Limitarlo a las decisiones solicitadas y enlazar la fuente funcional en lugar de repetir el dominio.
- [Los diseños podrían interpretarse como reglas funcionales] → Incluir explícitamente la jerarquía de fuentes y el procedimiento para contradicciones en el README.
- [Las tareas posteriores podrían omitir estas referencias] → Establecer en el README la revisión previa como regla para cambios de dominio o interfaz.

## Migration Plan

1. Crear exclusivamente la estructura documental bajo `docs/`.
2. Copiar los seis adjuntos a sus rutas finales, normalizando solo el nombre de la quinta imagen.
3. Añadir el índice visual y el resumen de decisiones técnicas.
4. Verificar existencia, hashes, tamaños, enlaces y apertura de PNG.

El cambio no requiere migración de datos ni despliegue. Para revertirlo basta retirar los nuevos archivos documentales, dado que no modifica estado ni componentes existentes.

## Open Questions

Ninguna para este cambio documental. Las dudas funcionales o de contratos externos permanecen sujetas a validación y no se resolverán a partir de los diseños.
