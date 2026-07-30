# Referencias visuales del flujo ciudadano

Estas imágenes son las referencias visuales principales para implementar las vistas del sistema de cancelación de certificados digitales. El flujo vigente contiene una pantalla de inicio y cinco pasos numerados.

## Flujo vigente

| Vista | Función |
| --- | --- |
| [`home.png`](./home.png) | Página de inicio, ingreso del DNI y consulta de existencia de certificados disponibles, sin datos individuales. |
| [`step-1.png`](./step-1.png) | Paso 1: autenticación del ciudadano mediante ID Perú. |
| [`step-2.png`](./step-2.png) | Paso 2: selección exclusiva de un certificado digital vigente. |
| [`step-3.png`](./step-3.png) | Paso 3: selección del motivo de cancelación. |
| [`step-4.png`](./step-4.png) | Paso 4: revisión de la información, consentimiento y confirmación. |
| [`step-5-final.png`](./step-5-final.png) | Paso 5: resultado de la revocación y constancia. |

## Consulta y selección de certificados

La consulta de la pantalla inicial y el listado detallado son servicios distintos:

- El primer servicio recibe el DNI y solo confirma si existen certificados disponibles. No devuelve lista, cantidad, número de orden, fecha de creación ni UUID.
- Después de autenticar al ciudadano, el segundo servicio devuelve las **emisiones vigentes**. En la interfaz cada emisión se presenta como un **certificado digital vigente** y contiene, como mínimo, número de orden, fecha de creación y UUID.

- Solo una confirmación positiva del primer servicio permite avanzar hacia la autenticación; un error o resultado incierto no equivale a ausencia.
- Una lista vacía del segundo servicio impide continuar, incluso si la consulta inicial fue positiva.
- La lista obtenida después de la autenticación debe conservarse vinculada con la solicitud de cancelación.
- Los certificados no se muestran antes de autenticar al ciudadano mediante ID Perú.
- Después de la autenticación siempre se presenta el paso de selección, incluso cuando exista un solo certificado.
- El ciudadano debe seleccionar exactamente un certificado para continuar. Incluso con un solo elemento, la elección debe ser expresa.
- Elegir otra tarjeta antes de continuar reemplaza la selección anterior; no existe la acción "seleccionar todos".
- Los certificados no seleccionados quedan fuera de la operación y permanecen vigentes.
- La revocación futura recibe únicamente el UUID seleccionado.
- Después de la confirmación, la selección queda inmutable.
- El servicio de revocación procesa ese certificado y devuelve un resultado exitoso, fallido o incierto.
- No existe un resultado parcial dentro de una solicitud de un solo certificado.
- La constancia debe identificar el certificado seleccionado y reflejar su resultado.

Estas reglas describen el comportamiento funcional esperado, pero no definen los contratos técnicos definitivos de los servicios externos.

## Inconsistencias conocidas en imágenes reutilizadas

Algunas imágenes conservan elementos del diseño anterior. Se mantienen sin modificación como referencias de composición, jerarquía visual y estilo; sus textos o steppers contradictorios no son reglas funcionales vigentes.

| Imagen | Inconsistencia conocida | Interpretación vigente |
| --- | --- | --- |
| `step-1.png` | Muestra un stepper de cuatro pasos que omite la selección de certificados. | Autenticación es el paso 1 de cinco y selección es el paso 2. |
| `step-2.png` | Muestra dos tarjetas seleccionadas y textos que permiten marcar uno o varios certificados. | Solo su composición, jerarquía y tarjetas son referencia. La implementación usa un grupo de radio, una sola tarjeta activa y textos singulares. |
| `step-3.png` | Presenta “Motivo” como paso 2 dentro de un flujo de cuatro pasos. | Motivo corresponde al paso 3 de cinco. |
| `step-4.png` | Presenta confirmación como paso 3 y afirma que se cancelarán todos los certificados asociados al DNI. | Confirmación corresponde al paso 4 y muestra únicamente el certificado seleccionado. |
| `step-5-final.png` | Presenta constancia como paso 4 y contiene textos plurales del flujo anterior. | Resultado y constancia corresponden al paso 5 y deben identificar el único certificado de la operación. |

La numeración y los textos internos se corregirán al implementar cada vista. No deben editarse ni regenerarse estas imágenes dentro de tareas documentales.

## Fuentes y reglas de uso

- [`PROJECT_CONTEXT.md`](../context/PROJECT_CONTEXT.md) es la única fuente funcional principal y vigente para comprender el dominio, alcance, actores, reglas de negocio, estados y restricciones.
- Las imágenes de este directorio son las referencias visuales principales para implementar las vistas.
- Los diseños no deben utilizarse para inventar reglas funcionales que no estén confirmadas en `PROJECT_CONTEXT.md`.
- Cuando exista una contradicción funcional entre una imagen y `PROJECT_CONTEXT.md`, prevalece `PROJECT_CONTEXT.md`. La diferencia debe registrarse como pendiente de validación o corrección de interfaz.
- Toda tarea posterior relacionada con el dominio o las interfaces debe revisar `PROJECT_CONTEXT.md` y la imagen correspondiente antes de implementar cambios.
- Los documentos que describen la implementación técnica actual pueden registrar temporalmente un comportamiento anterior, pero no sustituyen el contexto funcional vigente y deben señalar esa divergencia.

Las imágenes deben conservarse sin modificaciones, regeneraciones ni rediseños mientras funcionen como fuentes de referencia del proyecto.
