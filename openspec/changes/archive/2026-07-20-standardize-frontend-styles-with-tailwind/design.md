## Context

Tailwind CSS 4.3.2 y `@tailwindcss/postcss` ya están instalados y compilan correctamente. El layout raíz y los estados técnicos usan utilidades Tailwind, pero la página ciudadana implementada después introdujo un segundo modelo: nombres semánticos como `hero`, `consultation-card`, `primary-action` o `result-panel` cuyo diseño completo vive en una única línea extensa de `app/globals.css`. El encabezado también contiene un estilo inline para corregir el tamaño del logo.

El problema no es la existencia de CSS escrito a mano, sino la ausencia de un límite: una hoja global conoce la estructura interna de componentes, las modificaciones visuales requieren saltar entre TSX y CSS, los nombres pueden colisionar y las siguientes pantallas podrían ampliar el mismo bloque. La decisión técnica del proyecto establece Tailwind CSS y el repositorio ya demuestra que las utilidades funcionan.

La migración debe preservar exactamente el comportamiento funcional de la consulta de DNI, sus estados y su accesibilidad. También debe mantener los PNG adjuntos sin alterarlos y respetar `PROJECT_CONTEXT.md` y las referencias UI.

## Goals / Non-Goals

**Goals:**

- Establecer Tailwind-first como convención única y explícita para la presentación de componentes.
- Hacer visible la composición de cada elemento en el archivo TSX que lo representa.
- Reducir `globals.css` a responsabilidades realmente globales y pequeñas.
- Eliminar selectores globales dependientes de la estructura de componentes y estilos inline visuales.
- Conservar la apariencia actual aprobada en escritorio y vistas adaptables, junto con foco, estados de carga, errores y resultados.
- Mantener las dependencias y la arquitectura simples.

**Non-Goals:**

- Rediseñar la página de inicio o modificar las imágenes de referencia.
- Crear un design system completo, una biblioteca genérica de componentes o un catálogo amplio de tokens.
- Incorporar `clsx`, `tailwind-merge`, CSS Modules, Sass, styled-components, kits de UI o iconos externos.
- Cambiar lógica de elegibilidad, validación, navegación, HTTP, contratos o backend.
- Implementar las pantallas posteriores del flujo ciudadano.

## Decisions

### 1. Utilidades Tailwind colocadas junto al markup

Los archivos TSX expresarán layout, tamaño, color, tipografía, responsive, pseudoestados y accesibilidad visual mediante utilidades Tailwind. Los nombres globales de componente actuales se retirarán cuando termine la migración.

Esto permite revisar un componente sin reconstruir mentalmente una hoja global distante y hace explícitas las variantes responsive. Se descarta conservar el modelo actual porque perpetúa dos estrategias. También se descarta CSS Modules: solucionaría el alcance de selectores, pero no alinearía el proyecto con la decisión Tailwind-first y añadiría otra convención.

### 2. Límite estricto para `globals.css`

`globals.css` conservará `@import "tailwindcss"` y, solo si aportan reutilización real, una configuración `@theme` pequeña y reglas para elementos verdaderamente globales. No contendrá selectores de componentes, `@apply` para ocultar conjuntos de utilidades, ni una segunda hoja global.

Preflight cubrirá normalización y box sizing. El layout raíz aplicará mediante clases de `<body>` y sus landmarks la tipografía, fondo, altura mínima y foco que hoy se definen globalmente. Las animaciones usarán utilidades existentes de Tailwind cuando sean suficientes; una animación global nueva requerirá una necesidad no expresable razonablemente y documentación junto a la regla.

### 3. Tema mínimo, no sistema de diseño anticipado

Los pocos valores institucionales repetidos podrán exponerse mediante `@theme` con nombres del dominio visual, mientras los valores únicos permanecerán como utilidades arbitrarias cerca del elemento. No se crearán escalas completas, variantes de botón ni componentes genéricos sin más de un consumidor real.

Esto evita dos extremos: repetir códigos críticos por todas partes o convertir este refactor en la creación prematura de un design system.

### 4. Estados dinámicos con composición local simple

El formulario mantendrá las clases comunes como cadenas locales y agregará utilidades condicionales mediante template literals o expresiones pequeñas. No se añadirá una dependencia para composición porque el número de variantes actual es reducido. Las clases dinámicas deberán aparecer como literales completos detectables por Tailwind; no se construirán fragmentos como `text-${color}-700`.

### 5. Migración visualmente equivalente y verificable

Se migrará por superficies: shell técnico, encabezado y pie; home y beneficios; formulario y estados de resultado; errores y carga. Después se eliminarán los selectores ya sin consumidores. Las pruebas estáticas existentes seguirán verificando contenido y comportamiento, y se añadirá una prueba de convención que inspeccione el CSS global y el código fuente para impedir el regreso de estilos inline visuales y clases de componentes globales.

La equivalencia visual se revisará en al menos un viewport de escritorio, uno intermedio y uno móvil, incluyendo formulario inicial, validación, carga y resultados controlados. No se añadirá una dependencia E2E solo para este refactor.

### 6. Recursos gráficos y atributos propios de Next.js permanecen separados del estilo

Los `width` y `height` intrínsecos de `next/image` continuarán describiendo la proporción original del archivo; el tamaño renderizado se expresará con utilidades Tailwind. `priority`, `unoptimized`, `alt` y atributos ARIA conservarán su finalidad técnica o accesible y no se tratarán como presentación.

## Risks / Trade-offs

- **[Regresión visual por traducción de valores CSS]** → Migrar valor por valor, comparar viewports y estados antes de borrar el selector original, y ejecutar build después de la limpieza.
- **[TSX con cadenas de clases largas]** → Mantener componentes enfocados, formatear el markup y extraer únicamente subcomponentes con responsabilidad real; no ocultar las clases nuevamente en CSS global.
- **[Abuso de valores arbitrarios]** → Promover a `@theme` solo los valores repetidos y con significado, sin crear una taxonomía anticipada.
- **[Prueba de convención demasiado rígida]** → Comprobar límites concretos del repositorio y permitir excepciones explícitas documentadas, en lugar de prohibir todo CSS estándar.
- **[Mezcla temporal durante la migración]** → Completar y verificar cada superficie en la misma implementación; el resultado final no dejará dos fuentes de verdad para un mismo elemento.

## Migration Plan

1. Registrar el límite de estilos en la documentación frontend y preparar la prueba de convención.
2. Definir únicamente los tokens mínimos realmente repetidos.
3. Migrar layout, encabezado, pie y estados técnicos a utilidades Tailwind.
4. Migrar home, formulario, resultados y beneficios conservando cada estado funcional.
5. Sustituir el estilo inline del logo por clases responsive y retirar todos los selectores globales sin consumidores.
6. Ejecutar type-check, pruebas, build y revisión visual responsive de los estados principales.
7. Si una regresión no puede corregirse dentro del cambio, revertir la superficie afectada junto con sus selectores; no dejar una migración parcial sin documentar.

## Open Questions

Ninguna. La propuesta adopta Tailwind-first conforme a la decisión técnica existente y conserva un escape documentado para necesidades globales que Tailwind no represente razonablemente.
