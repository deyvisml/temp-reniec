## Why

Los resultados de la consulta de elegibilidad sustituyen actualmente todo el contenido del formulario dentro de la misma tarjeta. En el escenario sin certificados, un mensaje breve ocupa una superficie sobredimensionada, conserva la apariencia del formulario que lo originó y deja una jerarquía visual confusa. El ciudadano no distingue con suficiente claridad entre la acción que realizó y el resultado definitivo que debe comprender.

El problema se repite conceptualmente en los estados elegible, no elegible, no concluyente y de error. Corregir únicamente el texto o reducir la tarjeta mantendría dos responsabilidades dentro del mismo contenedor. Se necesita un patrón de feedback separado, consistente y accesible antes de continuar con las siguientes pantallas del flujo.

## What Changes

- Mantener visible la página de inicio y su formulario como contexto estable durante la consulta y presentar el resultado en una superficie independiente.
- Incorporar un diálogo modal de resultado compacto, institucional y responsive para los estados elegible, no elegible, no concluyente, servicio no disponible, timeout, pérdida de conexión, conflicto y error controlado.
- Implementar el diálogo con el elemento nativo `dialog`, React y utilidades Tailwind, sin incorporar SweetAlert, un kit de UI ni otra dependencia de producción.
- Diferenciar cada resultado mediante iconografía, título, explicación y acciones explícitas, sin depender únicamente del color ni exponer datos técnicos o certificados individuales.
- Para `NOT_ELIGIBLE`, comunicar de forma breve que no se encontraron certificados digitales disponibles para cancelar y ofrecer una acción convencional para aceptar el resultado, sin sugerir la prueba sucesiva de otros DNIs.
- Conservar las acciones seguras actuales: continuar solo cuando el backend autoriza, reintentar únicamente resultados reintentables y reiniciar el formulario cuando el resultado es terminal.
- Gestionar apertura, foco inicial, navegación por teclado, tecla Escape, restauración del foco y bloqueo del contenido de fondo conforme al comportamiento modal esperado.
- Evitar el cierre accidental por clic en el fondo; toda salida del diálogo debe corresponder a una acción segura y definida.
- Preservar contratos API, backend, persistencia, reglas de elegibilidad y navegación existentes; el cambio es exclusivamente de presentación e interacción frontend.
- Añadir pruebas del mapeo de estados, contenido, acciones, semántica accesible y renderizado responsive, además de una revisión visual en escritorio, ancho intermedio y móvil.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `citizen-eligibility-entry`: reemplaza los paneles de resultado incrustados en la tarjeta del formulario por un diálogo de feedback accesible, persistente y consistente para todos los resultados de elegibilidad.

## Impact

- Código afectado: `frontend/components/dni-eligibility-form.tsx` y, si aporta una responsabilidad concreta, un componente local `frontend/components/eligibility-outcome-dialog.tsx`.
- Pruebas afectadas: pruebas frontend de elegibilidad, renderizado, accesibilidad estática y convenciones Tailwind.
- Estilos: utilidades Tailwind colocadas junto al markup; `app/globals.css` no recuperará selectores de componentes.
- Dependencias: ninguna nueva. Se descarta SweetAlert porque el comportamiento requerido puede resolverse con APIs nativas y porque el proyecto exige mantener un conjunto mínimo de dependencias.
- Backend, MySQL, OpenAPI y contratos TypeScript: sin cambios previstos.
- Riesgo principal: una gestión incorrecta del foco podría dificultar el uso con teclado o lector de pantalla; se controla mediante `dialog.showModal()`, etiquetado explícito, acciones nativas y pruebas/revisión manual de foco.
