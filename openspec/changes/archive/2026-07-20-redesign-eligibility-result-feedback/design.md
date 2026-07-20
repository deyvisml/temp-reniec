## Context

La página de inicio contiene una única tarjeta destinada al ingreso del DNI. `DniEligibilityForm` también utiliza esa tarjeta para renderizar los resultados: cuando `view.kind !== "form"`, desmonta el formulario y lo reemplaza por un `ResultPanel` con altura mínima fija. En el resultado `NOT_ELIGIBLE`, el mensaje y una acción secundaria ocupan una superficie grande que visualmente sigue pareciendo el formulario. El patrón diluye el resultado, genera espacio vacío y mezcla entrada y feedback.

El contexto funcional establece que un ciudadano sin certificados susceptibles de cancelación no puede avanzar y debe finalizar el intento sin autenticación. El resultado es importante y requiere comprensión, por lo que un toast o alerta efímera no es apropiado. La fundación frontend exige semántica, teclado, foco visible, mensajes que no dependan del color y un conjunto mínimo de dependencias. El cambio Tailwind-first en curso también prohíbe volver a introducir estilos de componentes en `globals.css`.

## Goals / Non-Goals

**Goals:**

- Separar visual y semánticamente la captura del DNI del resultado de la consulta.
- Entregar una experiencia compacta, clara y coherente para todos los resultados inmediatos.
- Mantener el formulario como contexto reconocible detrás de una capa modal, sin que funcione como contenedor del resultado.
- Garantizar operación con teclado, lectores de pantalla, zoom y viewports móviles.
- Mantener acciones seguras y coherentes con cada resultado del backend.
- Resolver la necesidad con React, HTML nativo y Tailwind, sin una librería de alertas.

**Non-Goals:**

- Cambiar reglas de DNI, elegibilidad, creación o recuperación de solicitudes.
- Modificar backend, MySQL, OpenAPI, mocks o contratos TypeScript.
- Implementar ID Perú, JWT o pantallas posteriores.
- Crear un sistema general de modales, notificaciones o diseño.
- Utilizar toasts para resultados que requieren comprensión o acción.
- Rediseñar el hero, encabezado, formulario inicial o imágenes de referencia.

## Decisions

### 1. El resultado se presentará en un diálogo modal nativo

Se creará un componente enfocado `EligibilityOutcomeDialog` basado en `<dialog>` y abierto mediante `showModal()`. El diálogo se renderizará como hermano del formulario dentro del componente cliente, no como reemplazo del contenido de la tarjeta. La página y el formulario permanecerán visibles como contexto, atenuados e inertes por el comportamiento modal nativo.

El panel tendrá una anchura acotada cercana a 440–500 px, margen seguro en móvil, radio moderado, jerarquía tipográfica breve y una sola agrupación de acciones. El fondo utilizará una capa oscura translúcida suficiente para separar planos. No se usará una tarjeta de resultado de ancho completo, glassmorphism decorativo, una sombra exagerada ni grandes espacios vacíos.

Alternativas descartadas:

- **Toast o SweetAlert efímero:** el resultado puede perderse antes de comprenderse y no ofrece el nivel de persistencia requerido.
- **SweetAlert2 como modal:** añade una dependencia de producción, introduce una apariencia genérica ajena al proyecto y no elimina la necesidad de validar foco, semántica y responsive.
- **Banner inline debajo del formulario:** conserva simultáneamente dos focos de atención y puede quedar fuera del viewport después del envío.
- **Página de resultado separada:** agrega navegación y una ruta para un resultado inmediato sin aportar valor suficiente en este punto del flujo.

### 2. Un único componente representará variantes explícitas, no HTML duplicado

El estado funcional seguirá siendo la unión discriminada existente. Una función pura transformará cada variante en un modelo de presentación con `tone`, icono, título, descripción, acción principal y acción secundaria opcional. Las variantes serán literales y exhaustivas para que TypeScript detecte resultados sin representación.

Se utilizará el mismo vocabulario de botones, foco e iconos ya existente. No se creará un componente modal genérico sin consumidor real ni una API abstracta para casos futuros.

### 3. Las acciones dependerán de la capacidad real de continuar o reintentar

| Resultado | Acción principal | Acción secundaria |
| --- | --- | --- |
| Elegible y autorizado | Continuar con la verificación | Volver al inicio |
| No elegible | Aceptar | Ninguna |
| No concluyente | Reintentar consulta | Volver al inicio |
| Indisponibilidad, timeout o red | Reintentar consulta | Volver al inicio |
| Conflicto reintentable | Reintentar o recuperar según el contrato actual | Volver al inicio |
| Error no reintentable | Volver al formulario | Ninguna |

`NOT_ELIGIBLE` seguirá bloqueando la continuidad y no mostrará certificados individuales. El texto evitará presentar la ausencia como un fallo grave y aclarará que la consulta no modificó el DNI ni la identidad.

El clic sobre el backdrop no cerrará el diálogo. Escape ejecutará la salida segura equivalente a volver o reiniciar, nunca continuará ni reintentará automáticamente. La acción de reintento será siempre explícita.

### 4. El foco y los anuncios forman parte del estado, no son efectos visuales secundarios

El diálogo tendrá `aria-labelledby` y `aria-describedby`; el resultado esperado utilizará semántica de diálogo y los errores urgentes podrán utilizar `alertdialog` únicamente cuando corresponda. Al abrirse, el foco se dirigirá al encabezado enfocable o a la acción principal conforme a la variante. El comportamiento nativo contendrá el foco dentro del modal.

Al cerrarse para reiniciar, el foco volverá al campo DNI. Si la acción navega, no se restaurará artificialmente. El evento `cancel` se interceptará para convertir Escape en la acción segura de la variante. El resultado no se anunciará simultáneamente mediante múltiples regiones vivas que provoquen lecturas duplicadas.

### 5. La presentación será Tailwind-first y responsive

El componente utilizará utilidades Tailwind literales junto al markup, incluidas variantes `backdrop:*`, estados de foco y ajustes móviles. `globals.css` no recibirá selectores de diálogo ni animaciones específicas. El diálogo funcionará sin animación de entrada; se prioriza una aparición inmediata y estable sobre motion decorativo. Hover, active y focus conservarán transiciones breves y `prefers-reduced-motion`.

En móvil el diálogo mantendrá al menos 16 px respecto del viewport, permitirá desplazamiento interno cuando el contenido no quepa y conservará objetivos táctiles de al menos 44 px. No debe provocar scroll horizontal ni ocultar acciones con el teclado virtual o zoom al 200 %.

### 6. La verificación cubrirá comportamiento, semántica y composición visual

Las pruebas unitarias verificarán el mapeo exhaustivo de resultados, textos y acciones seguras. Las pruebas de renderizado estático comprobarán `dialog`, nombres/descripciones accesibles y ausencia del antiguo panel de resultado de ancho completo. No se añadirá jsdom ni una dependencia E2E solo para este cambio.

La revisión en navegador comprobará apertura, foco, Tab/Shift+Tab, Escape, restauración del foco, click no dismissivo sobre el fondo, reintento, reinicio y navegación autorizada. Se revisarán `NOT_ELIGIBLE`, un resultado reintentable y `ELIGIBLE` en escritorio, ancho intermedio y móvil.

## Risks / Trade-offs

- **[Soporte o comportamiento desigual de `<dialog>`]** → El conjunto actual de navegadores modernos soporta el elemento; se mantendrá markup comprensible y acciones nativas aun sin efectos adicionales.
- **[El modal interrumpe el flujo]** → La interrupción es deliberada solo después de una consulta y para un resultado que requiere decisión; carga y validación permanecen inline.
- **[Lectura duplicada por tecnologías asistivas]** → Usar una única estrategia de anuncio basada en foco y etiquetado del diálogo, evitando combinar indiscriminadamente `role=alert`, `aria-live` y `alertdialog`.
- **[Pérdida accidental del resultado]** → No cerrar mediante backdrop y mapear Escape a una salida segura y explícita.
- **[Exceso de abstracción]** → Mantener un componente específico de elegibilidad y una función de mapeo local; no crear infraestructura genérica de overlays.
- **[Regresión del formulario detrás del diálogo]** → Mantenerlo montado, deshabilitado durante la consulta y restaurarlo únicamente mediante transiciones de estado existentes y probadas.

## Migration Plan

1. Extraer el modelo de presentación exhaustivo para los estados de elegibilidad existentes.
2. Implementar `EligibilityOutcomeDialog` con semántica, foco y acciones seguras.
3. Mantener el formulario montado y sustituir el retorno anticipado de `ResultPanel` por el diálogo condicional.
4. Retirar constantes y markup del panel antiguo que queden sin consumidores.
5. Añadir pruebas de estados, contenido, acciones y semántica.
6. Ejecutar type-check, pruebas, build y revisión visual/teclado en los escenarios definidos.

Rollback: restaurar el renderizado condicional anterior dentro de `DniEligibilityForm`. No existen cambios de datos, API o dependencias que requieran reversión adicional.

## Open Questions

- Los textos exactos de atención ciudadana podrán recibir validación institucional posterior; la implementación mantendrá mensajes configurados localmente y no inventará canales de soporte.
