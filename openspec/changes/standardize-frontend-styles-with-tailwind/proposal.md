## Why

El frontend declara Tailwind CSS como tecnología obligatoria, pero la página ciudadana, el formulario y el shell institucional concentran casi toda su presentación en selectores de componentes dentro de `app/globals.css`, mientras otras vistas sí utilizan utilidades Tailwind. Esta mezcla carece de una regla explícita, oculta la composición visual fuera de los componentes y ha producido un stylesheet global monolítico, clases acopladas globalmente y estilos inline puntuales.

El cambio es necesario ahora, antes de implementar las siguientes pantallas del flujo, para establecer una convención única y evitar que el mismo patrón se replique en cada vista.

## What Changes

- Adoptar Tailwind CSS como mecanismo principal para layout, espaciado, tipografía, color, responsive, estados interactivos y composición visual del frontend.
- Migrar el shell, la página de inicio, el formulario de DNI, sus resultados, el encabezado, el pie y los estados técnicos existentes desde clases CSS globales propias hacia utilidades Tailwind colocadas junto al markup.
- Reducir `app/globals.css` a la importación de Tailwind y a verdaderos estilos globales: configuración temática mínima, comportamiento base compartido y animaciones globales justificadas.
- Prohibir que `globals.css` actúe como hoja de estilos de componentes, así como recrear componentes mediante `@apply`, estilos inline visuales o archivos CSS globales adicionales.
- Permitir CSS personalizado solo cuando Tailwind no represente razonablemente una necesidad global o una técnica especial, documentando y acotando la excepción.
- Conservar la apariencia aprobada, los recursos gráficos originales, el responsive, la accesibilidad, los estados funcionales y los contratos actuales; este cambio no rediseña la vista ni altera el flujo de elegibilidad.
- Mantener el conjunto actual de dependencias, sin incorporar librerías de composición de clases, kits de UI ni un sistema de diseño anticipado.
- Añadir verificaciones que detecten regresiones hacia selectores de componentes en `globals.css`, estilos inline visuales o pérdida de las utilidades Tailwind esenciales.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `frontend-foundation`: establece una estrategia Tailwind-first explícita, delimita el contenido permitido en el CSS global y define las reglas de mantenimiento para estilos futuros.
- `citizen-eligibility-entry`: exige que la página de inicio y todos los estados del formulario conserven su comportamiento y referencia visual al migrar a utilidades Tailwind.

## Impact

- Código afectado: `frontend/app/*.tsx`, `frontend/components/*.tsx`, `frontend/app/globals.css` y pruebas frontend relacionadas con renderizado y convenciones de estilo.
- Documentación afectada: `frontend/README.md` y las especificaciones de fundación y entrada ciudadana.
- APIs, backend, MySQL, OpenAPI y lógica funcional: sin cambios.
- Dependencias: sin incorporaciones ni eliminaciones previstas; se conserva Tailwind CSS 4 con `@tailwindcss/postcss`.
- Riesgo principal: regresión visual durante una migración mecánica; se controla mediante comparación responsive, pruebas existentes y revisión de los estados del formulario.
