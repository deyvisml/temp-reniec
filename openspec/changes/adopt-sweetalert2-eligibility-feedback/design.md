## Context

El frontend ya distingue resultados elegibles, no elegibles, inconclusos y errores controlados mediante un modelo tipado. La presentación actual utiliza un componente React propio basado en `<dialog>` que también implementa apertura, cierre, foco, acciones, estilos responsive e iconografía.

El proyecto busca reducir este mantenimiento propio. SweetAlert2 11.x es una librería MIT, tipada, independiente del framework y especializada en diálogos de resultados. La integración debe conservar el comportamiento funcional existente sin convertir SweetAlert2 en un sistema genérico de notificaciones ni utilizar alertas efímeras para resultados importantes.

Este cambio sucede al cambio completado `redesign-eligibility-result-feedback`: aquel debe archivarse primero y este cambio después, porque la presente especificación reemplaza su decisión de no usar una librería de alertas.

## Goals / Non-Goals

**Goals:**

- Delegar en SweetAlert2 la superficie modal, superposición, adaptación al viewport y comportamiento base de teclado.
- Mantener un mapeo exhaustivo y testeable entre resultados del dominio y contenido/acciones ciudadanas.
- Conservar el formulario montado, la limpieza del DNI, los reintentos seguros, la continuación autorizada y la correlación.
- Integrar la apariencia institucional con opciones públicas de SweetAlert2 y clases Tailwind literales.
- Mantener WCAG 2.2 AA como objetivo, incluidos foco visible, nombre accesible, acciones alcanzables y movimiento reducido.

**Non-Goals:**

- Adoptar un kit completo de componentes o un segundo sistema de diseño.
- Introducir toasts para resultados que requieren lectura o decisión.
- Crear un servicio universal de alertas antes de tener más consumidores reales.
- Cambiar estados, contratos HTTP, backend, base de datos o reglas de elegibilidad.
- Usar HTML dinámico sin sanitizar para construir el mensaje.

## Decisions

### SweetAlert2 será la única dependencia nueva

Se añadirá `sweetalert2` en una versión estable 11.x compatible, registrada por el lockfile. No se añadirá `sweetalert2-react-content`: los resultados no necesitan renderizar árboles React dentro del popup y la API de promesas de SweetAlert2 es suficiente.

Se descarta mantener `<dialog>` propio porque conserva responsabilidad local sobre casos que motivan el cambio. También se descarta incorporar un kit como Material UI, Chakra o shadcn porque ampliaría mucho el alcance y duplicaría el sistema visual actual.

### Una integración específica conservará el modelo funcional

El componente modal propio será reemplazado por un presentador específico de elegibilidad que:

1. recibe el `EligibilityOutcome` actual;
2. obtiene una configuración exhaustiva de título, texto, icono y acciones;
3. abre una única instancia con `Swal.fire` desde el cliente;
4. traduce la resolución de la promesa a `onContinue`, `onRetry` u `onReset`;
5. cierra únicamente su instancia durante la limpieza.

El mapeo de resultados seguirá separado de la llamada a la librería para poder probar las decisiones ciudadanas sin un navegador. No se expondrá SweetAlert2 en la lógica de negocio ni en el cliente HTTP.

### Se utilizará la API de texto y configuración pública

El título y la descripción utilizarán `titleText` y `text`. Los valores dinámicos no se interpolarán como HTML. Si existe identificador de correlación, se presentará mediante un nodo de texto seguro o una opción pública equivalente, nunca mediante una cadena HTML construida con datos externos.

La configuración usará `allowOutsideClick: false`, acciones explícitas y cierre por Escape traducido siempre a la acción segura de retorno. La continuación y el reintento nunca se ejecutarán por un cierre implícito.

### La personalización será limitada y localizable

El CSS distribuido por SweetAlert2 se importará una sola vez desde la raíz de la aplicación. `buttonsStyling: false` permitirá aplicar clases Tailwind literales mediante `customClass` para botones, popup y texto. No se agregarán selectores internos de SweetAlert2 ni reglas de componente a `globals.css`.

La configuración mantendrá un ancho acotado con margen móvil, acciones de al menos 44 px, orden responsive y foco institucional visible. Las animaciones se desactivarán cuando `prefers-reduced-motion: reduce` esté activo mediante configuración de apertura/cierre, sin parchear internamente la librería.

### La navegación permanecerá bajo control de Next.js

SweetAlert2 no generará enlaces ni decidirá rutas. Una confirmación elegible devolverá el control al presentador y este utilizará la ruta ya calculada para navegar. Los cierres, cancelaciones y resultados incompletos no podrán continuar el flujo.

## Risks / Trade-offs

- **[Dependencia adicional y aumento del bundle]** → importar SweetAlert2 solo desde el componente cliente que muestra resultados y comprobar el impacto del build; no añadir el adaptador React adicional.
- **[La apariencia por defecto puede no coincidir con RENIEC]** → usar únicamente las opciones públicas y `customClass`, con una personalización deliberadamente pequeña que no dependa del DOM interno.
- **[Colisión entre instancias globales]** → permitir una sola presentación de elegibilidad por formulario, cerrar la instancia al cambiar el estado y verificar que una resolución antigua no ejecute acciones.
- **[Actualizaciones mayores pueden cambiar comportamiento]** → permanecer en la línea 11.x, conservar el lockfile y cubrir los escenarios críticos con pruebas y revisión real en navegador.
- **[La librería no garantiza por sí sola la accesibilidad del producto]** → verificar nombre/descripción, foco, teclado, Escape, zoom, movimiento reducido y viewport móvil como criterios del proyecto.

## Migration Plan

1. Archivar primero `redesign-eligibility-result-feedback` para consolidar sus requisitos funcionales.
2. Instalar SweetAlert2 y registrar la versión resuelta en el lockfile.
3. Incorporar el presentador específico y migrar todos los resultados sin cambiar `ViewState` ni callbacks.
4. Importar el CSS del proveedor en la raíz y aplicar personalización mediante opciones públicas y Tailwind.
5. Eliminar `EligibilityOutcomeDialog` y sus pruebas de implementación propia después de cubrir el reemplazo.
6. Ejecutar pruebas, contrato, build y revisión responsive/teclado.
7. Archivar este cambio después del cambio predecesor para que la especificación final sustituya la restricción anterior.

El rollback consiste en restaurar el componente anterior, retirar la dependencia y revertir el lockfile; no existe migración de datos ni coordinación con backend.

## Open Questions

Ninguna. La implementación utilizará SweetAlert2 11.x y conservará los textos y acciones funcionales ya aprobados.
