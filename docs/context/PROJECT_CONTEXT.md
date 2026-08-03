# Contexto del proyecto

## 1. Resumen ejecutivo

El proyecto consiste en el diseño e implementación de un sistema web institucional para el **Registro Nacional de Identificación y Estado Civil (RENIEC) del Perú**, dirigido a ciudadanos que actúan como personas naturales.

Su propósito es proporcionar un canal digital de autoservicio mediante el cual el titular pueda **revocar de forma inmediata una credencial digital vigente asociado a su DNI por cada solicitud**, cuando exista una situación que pueda comprometer su seguridad, como pérdida, robo, cambio de equipo o sospecha de uso no autorizado.

## Regla vigente de unidad de operación

Cada solicitud selecciona, confirma y posteriormente revoca **exactamente una credencial digital vigente**. El segundo servicio devuelve todas las credenciales asociadas al DNI autenticado, incluidas las ya revocadas con su fecha de revocación, pero el ciudadano solo puede elegir una vigente. Antes de confirmar puede reemplazarla; después de confirmar queda inmutable. Las demás credenciales permanecen fuera de la operación.

En la comunicación dirigida al ciudadano se utilizará preferentemente el término **revocación de credenciales digitales**. Desde la perspectiva técnica y del dominio de certificación digital, la operación ejecutada corresponde a una **revocación**.

Un ciudadano puede tener una o más **emisiones vigentes de credenciales digitales** asociadas a su DNI. Cada emisión corresponde a una credencial generado en un momento determinado y puede identificarse mediante un número de orden, una fecha de creación y un UUID único. En la interfaz ciudadana, estas emisiones se presentarán como **credenciales digitales vigentes**.

El sistema no revoca la identidad civil, el número de DNI, el documento físico, el DNI electrónico ni la cuenta de ID Perú. Su objeto funcional es la credencial digital vigente seleccionada expresamente por el ciudadano en la solicitud actual.

El flujo general comprende una pantalla de inicio y cinco pasos:

1. Ingreso del número de DNI y consulta de existencia de credenciales digitales disponibles.
2. Autenticación del titular mediante ID Perú.
3. Selección de una credencial digital vigente.
4. Selección del motivo de revocación.
5. Confirmación informada de la operación.
6. Ejecución inmediata de la revocación.
7. Presentación del resultado y generación de una constancia o comprobante.

El sistema dependerá de dos servicios de credenciales claramente separados, además de ID Perú y del servicio de revocación. El primer servicio se consumirá desde la pantalla de inicio y responderá únicamente si existe al menos una credencial vigente disponible para revocar, sin devolver lista, cantidad, número de orden, fecha de creación ni UUID. Después de autenticar al ciudadano, un segundo servicio devolverá la lista detallada de credenciales vigentes y revocadas. Las revocadas serán informativas; el ciudadano elegirá exactamente una vigente y el servicio de revocación recibirá su UUID en una operación individual e idempotente.

---

## 2. Antecedentes y contexto

RENIEC es la institución pública responsable de la identificación de las personas y del registro de los hechos relativos al estado civil en el Perú. Dentro de su ámbito de identidad y certificación digital, los ciudadanos pueden contar con credenciales digitales vinculados a su identificación y al uso de servicios digitales.

Las credenciales digitales pueden utilizarse para acreditar identidad en entornos digitales, realizar operaciones de autenticación y ejecutar funciones relacionadas con la firma digital. Mientras permanecen vigentes, pueden ser aceptados por sistemas y servicios que confían en ellos.

Un mismo ciudadano puede contar con varias emisiones vigentes de credenciales digitales generadas en diferentes momentos. Esto no implica necesariamente que existan diferentes tipos de credencial, sino que pueden coexistir varias credenciales vigentes asociados al mismo titular, cada uno con su propia fecha de creación y su propio identificador único.

Determinadas situaciones pueden hacer necesario que el titular impida la utilización de uno o varios de estos credenciales, especialmente cuando:

- Pierde el dispositivo o elemento relacionado con su uso.
- Sufre el robo de dicho dispositivo.
- Cambia de equipo o número asociado.
- Sospecha que alguno de sus mecanismos digitales fue comprometido.
- Identifica una emisión que ya no desea mantener vigente.
- Presenta otra circunstancia que justifica la revocación.

El proyecto busca establecer un canal remoto, comprensible y seguro para que una persona natural pueda consultar la existencia de credenciales digitales vigentes, autenticar su identidad, seleccionar cuáles desea revocar y obtener evidencia del resultado de la operación.

La base normativa específica, las políticas institucionales aplicables, las responsabilidades operativas y el procedimiento vigente de revocación todavía deben ser documentados y validados formalmente.

---

## 3. Definición del problema

### 3.1. Situación identificada

Un ciudadano puede tener uno o varias credenciales digitales vigentes asociados a su DNI, generados en momentos distintos. Estos credenciales pueden continuar siendo válidos aun cuando se produzca una situación de riesgo, como pérdida, robo, cambio de equipo o sospecha de acceso no autorizado.

En estos casos, el titular necesita contar con un mecanismo que le permita identificar las emisiones vigentes asociadas a su DNI y revocar únicamente aquellas que considere comprometidas o que ya no desee mantener activas.

No se dispone todavía de información confirmada sobre la forma en que este procedimiento se realiza actualmente, si existe un canal digital previo, si requiere atención presencial o si depende de intervención administrativa.

### 3.2. Dificultades preliminares

A partir de la información disponible, se identifican las siguientes dificultades:

- El ciudadano podría no contar con un canal digital directo para revocar sus credenciales.
- La demora en la revocación podría prolongar el periodo de exposición ante un posible uso no autorizado.
- Los conceptos de identidad digital, DNI electrónico, credencial digital, emisión y credencial digital pueden resultar confusos.
- El titular no necesariamente conoce cuántas emisiones vigentes tiene asociadas a su DNI.
- El ciudadano necesita distinguir las credenciales por datos simples, sin depender de detalles criptográficos o técnicos.
- La operación requiere consultar previamente las credenciales susceptibles de revocación.
- La revocación debe ser autorizada exclusivamente por el titular.
- El sistema debe permitir seleccionar exactamente una credencial por solicitud.
- La operación debe producir el resultado del único credencial confirmada.
- El ciudadano necesita recibir evidencia clara de la credencial seleccionada y de su resultado.

### 3.3. Causas preliminares

Las causas del problema todavía requieren análisis formal. Como hipótesis iniciales se consideran:

- Ausencia o insuficiencia de un canal de autoservicio especializado.
- Dependencia de procesos operativos que podrían no ser inmediatos.
- Complejidad técnica del dominio de certificación digital.
- Falta de claridad sobre la coexistencia de varias emisiones vigentes.
- Necesidad de integrar servicios diferentes para consultar, autenticar y revocar.
- Necesidad de preservar la atomicidad, la idempotencia y el tratamiento seguro de resultados inciertos.

### 3.4. Consecuencias posibles

Mientras uno o varias credenciales permanezcan vigentes después de una situación de riesgo, podrían producirse:

- Intentos de autenticación no autorizada.
- Uso indebido de mecanismos de identificación digital.
- Riesgo de operaciones realizadas sin autorización del titular.
- Incertidumbre sobre cuáles credenciales continúan vigentes.
- Revocación innecesaria de credenciales que el ciudadano todavía desea conservar.
- Dificultad para acreditar qué credenciales fueron revocadas.
- Pérdida de confianza en los servicios digitales institucionales.

Estas consecuencias son interpretaciones derivadas del propósito del proyecto y deberán alinearse con el alcance real de las credenciales involucrados.

### 3.5. Necesidad que justifica el sistema

Se requiere un servicio web que permita al ciudadano:

- Ingresar su número de DNI.
- Consultar si cuenta con credenciales digitales vigentes susceptibles de revocación.
- Impedir la continuidad cuando no exista ninguna credencial disponible.
- Demostrar que es el titular mediante ID Perú.
- Visualizar las emisiones vigentes mediante información comprensible.
- Seleccionar una credencial digital vigente.
- Registrar el motivo de la operación.
- Comprender las consecuencias antes de confirmar.
- Ejecutar la revocación de la credencial seleccionada.
- Consultar el resultado de la operación confirmada.
- Obtener una constancia verificable del resultado.

---

## 4. Objetivo general del proyecto

Implementar un servicio web institucional de RENIEC que permita a una persona natural revocar de manera segura e inmediata una credencial digital vigente asociado a su DNI por solicitud, previa consulta de las emisiones disponibles, autenticación del titular mediante ID Perú, selección expresa de la credencial, registro del motivo y confirmación de la operación.

---

## 5. Objetivos específicos preliminares

- Permitir que el ciudadano inicie el proceso utilizando su número de DNI.
- Consultar mediante un primer servicio institucional si el DNI tiene credenciales disponibles para revocar.
- Evitar que el proceso continúe cuando el primer servicio confirme que no existen credenciales disponibles.
- Distinguir la ausencia confirmada de un resultado inconcluso, indisponibilidad, timeout o error técnico.
- Autenticar al titular mediante el servicio externo ID Perú.
- Verificar la correspondencia entre la identidad autenticada y el DNI ingresado.
- Consultar mediante un segundo servicio la lista detallada de credenciales vigentes y revocadas después de autenticar al titular.
- Recibir y conservar temporalmente la lista vinculada con la solicitud.
- Mostrar las credenciales digitales vigentes después de la autenticación.
- Permitir seleccionar exactamente una credencial.
- Mostrar siempre el paso de selección, incluso cuando exista un solo credencial.
- Exigir exactamente una selección antes de continuar.
- Registrar el motivo de la revocación.
- Incorporar una opción abierta para motivos no contemplados en el catálogo.
- Informar al ciudadano sobre el carácter inmediato y las consecuencias de la operación.
- Obtener una confirmación explícita antes de ejecutar la revocación.
- Enviar al servicio de revocación el UUID seleccionado.
- Recibir el resultado del único credencial procesado.
- Comunicar un resultado exitoso, fallido o incierto, sin estados parciales.
- Generar una constancia o comprobante coherente con el resultado real.

---

## 6. Descripción general de la solución

La solución propuesta es un **portal web de revocación de credenciales digitales vigentes para personas naturales**.

En la pantalla de inicio, el ciudadano ingresará su número de DNI. El sistema consultará un primer servicio externo que responderá únicamente si existen credenciales digitales disponibles para revocar. Un resultado negativo confirmado impedirá continuar; un resultado positivo permitirá iniciar la autenticación. Un resultado inconcluso, indisponibilidad, timeout o error técnico no se interpretará como ausencia de credenciales y mantendrá bloqueada la continuidad con una opción de reintento segura.

La consulta inicial no devolverá ni persistirá credenciales individuales, cantidades, números de orden, fechas de creación o UUID.

Después de autenticar su identidad mediante ID Perú, el sistema consumirá un segundo servicio para obtener la lista real de credenciales vigentes y revocadas. Cada elemento incluirá número de orden, fecha de creación, UUID, estado y fecha de revocación cuando corresponda. La lista completa quedará vinculada con la solicitud y se mostrará en el paso de selección. El usuario deberá seleccionar exactamente una credencial vigente para continuar; las revocadas serán únicamente informativas.

La confirmación positiva del primer servicio establece que debe existir al menos una credencial vigente en el listado posterior. Si el segundo servicio devuelve una lista vacía o sin vigentes, el ciudadano no podrá continuar y la respuesta se tratará como una violación del contrato externo, sin inventar credenciales ni alterar la autenticación ya registrada.

Posteriormente, el titular seleccionará el motivo de la revocación. El sistema ofrecerá un conjunto de motivos predefinidos y una alternativa denominada **Otro motivo**, que permitirá registrar una explicación adicional.

Antes de ejecutar la operación, el sistema presentará un resumen que incluirá la credencial seleccionada, el motivo registrado y las advertencias correspondientes. La revocación solo se solicitará después de la confirmación expresa del ciudadano. Desde ese momento, la selección queda inmutable.

El servicio de revocación recibirá el UUID seleccionado bajo una única clave de idempotencia. Un éxito confirma su revocación, un fallo confirma que no fue revocado y un resultado incierto deberá reconciliarse sobre la misma operación.

Cuando el proveedor confirme el éxito, el sistema esperará el periodo configurado de propagación antes de generar la constancia. Durante ese tiempo mostrará el progreso en el paso 4 y continuará el procesamiento en backend aunque el ciudadano cierre o recargue la pestaña. Solo con la constancia disponible mostrará el paso 5.

---

## 7. Alcance funcional preliminar

### 7.1. Procesos incluidos

El alcance preliminar comprende:

- Ingreso y validación básica del número de DNI.
- Consulta inicial de existencia de credenciales disponibles mediante un resultado positivo o negativo.
- Manejo separado de resultado inconcluso, indisponibilidad, timeout y error técnico.
- Autenticación mediante ID Perú.
- Validación de correspondencia entre el DNI y la identidad autenticada.
- Consulta posterior de la lista detallada de credenciales digitales vigentes.
- Conservación temporal de la lista dentro de la solicitud.
- Visualización de credenciales digitales vigentes.
- Selección de una credencial.
- Validación de selección única.
- Registro del motivo de revocación.
- Registro de un motivo personalizado mediante la opción Otro.
- Presentación de advertencias y consecuencias.
- Confirmación expresa de la revocación.
- Envío del UUID seleccionado al servicio de revocación.
- Recepción de un resultado para la credencial seleccionada.
- Determinación del resultado general de la operación.
- Presentación del resultado.
- Generación y descarga de una constancia o comprobante.
- Manejo general de respuestas negativas, errores y falta de disponibilidad.

### 7.2. Acciones generales disponibles para el ciudadano

- Ingresar su DNI.
- Continuar o abandonar el proceso.
- Autenticarse mediante ID Perú.
- Revisar las credenciales digitales vigentes.
- Seleccionar una credencial.
- Desmarcar credenciales antes de confirmar.
- Seleccionar un motivo.
- Escribir un motivo alternativo.
- Revisar la información antes de confirmar.
- Confirmar la revocación.
- Consultar el resultado de la operación.
- Descargar una constancia.
- Finalizar el proceso.

### 7.3. Elementos fuera del alcance preliminar

No forman parte del alcance confirmado:

- Emisión de nuevos credenciales digitales.
- Reactivación de credenciales revocadas.
- Revocación del número de DNI.
- Revocación o bloqueo del documento físico.
- Eliminación de la identidad civil del ciudadano.
- Revocación de la cuenta de ID Perú.
- Administración general de identidades digitales.
- Gestión de usuarios institucionales.
- Módulos administrativos o de atención interna.
- Recuperación de credenciales.
- Gestión general de dispositivos.
- Cierre de sesiones en otros sistemas.
- Bloqueo general de todos los servicios digitales del ciudadano.
- Modificación de los datos técnicos de una credencial.
- Consulta pública de credenciales de terceros.
- Selección basada en tipos de credencial, salvo que posteriormente se confirme esa necesidad.

### 7.4. Aspectos del alcance pendientes de confirmación

- Tipo exacto de credenciales incluidos.
- Criterio utilizado para considerar una credencial vigente y cancelable.
- Tratamiento de credenciales vencidas, suspendidas o previamente revocadas.
- Nombre y significado exacto del número de orden.
- Formato exacto de la fecha de creación.
- Datos adicionales que podría devolver el servicio de consulta.
- Si el UUID debe mostrarse completo, parcialmente oculto o mediante un identificador amigable.
- Revalidación de la lista después de la autenticación.
- Posibilidad de que una credencial cambie de estado durante el proceso.
- Alcance exacto de los efectos posteriores.
- Contenido y validez de la constancia.
- Existencia de canales de soporte o derivación.
- Reglas para reintentos y resultados inciertos.

---

## 8. Usuarios y actores involucrados

| Actor | Rol dentro del proceso | Necesidad principal | Acciones o responsabilidades | Situaciones de participación |
|---|---|---|---|---|
| Ciudadano o persona natural | Usuario principal y titular de las credenciales digitales | Revocar oportunamente una credencial vigente por solicitud | Ingresar DNI, autenticarse, seleccionar una credencial, registrar motivo, confirmar y descargar la constancia | Robo, pérdida, cambio de equipo o número, sospecha de uso no autorizado u otro motivo |
| RENIEC | Institución responsable del servicio | Proporcionar un canal confiable y trazable | Administrar el servicio, definir reglas, comunicar resultados y garantizar la trazabilidad institucional | Durante todo el proceso |
| ID Perú | Servicio externo de autenticación | Confirmar que la persona que realiza el proceso es quien afirma ser | Autenticar al ciudadano y devolver el resultado correspondiente | Después de verificar que existe al menos una credencial |
| Servicio de existencia de credenciales | Sistema externo colaborador | Confirmar si el DNI tiene al menos una credencial disponible para revocar | Recibir el DNI y devolver únicamente un resultado positivo o negativo, sin lista ni datos individuales | Desde la pantalla de inicio |
| Servicio de listado de credenciales vigentes | Sistema externo colaborador | Obtener las emisiones vigentes después de autenticar al ciudadano | Recibir la referencia necesaria y devolver una lista con número de orden, fecha de creación y UUID | En el paso 2, después de ID Perú |
| Servicio de revocación | Sistema externo colaborador | Ejecutar la revocación de la credencial seleccionada | Recibir un UUID y una clave de idempotencia, y devolver su resultado | Después de la confirmación |
| Servicio o mecanismo de constancias | Actor conceptual pendiente de validación | Generar evidencia de la operación | Producir o proporcionar el comprobante correspondiente | Después de contar con el resultado de la revocación |
| Personal de soporte o atención | Actor potencial pendiente de validación | Atender incidencias o casos no resueltos en línea | Orientar, revisar incidencias o derivar casos | Errores, indisponibilidad o resultados inciertos |

---

## 9. Escenarios principales de uso

### 9.1. DNI sin credenciales digitales vigentes

El ciudadano ingresa su DNI y el primer servicio confirma que no existen credenciales disponibles para revocar. El sistema informa el resultado y finaliza el proceso sin solicitar autenticación ni crear registros de credenciales.

### 9.2. DNI con un solo credencial vigente

El primer servicio confirma que existen credenciales. El ciudadano se autentica y el segundo servicio devuelve un único credencial. El ciudadano accede igualmente al paso de selección; la credencial no debe omitirse ni revocarse automáticamente y debe seleccionarlo de forma expresa.

### 9.3. DNI con varias credenciales vigentes

El primer servicio confirma que existen credenciales. Después de autenticarse, el segundo servicio devuelve varias emisiones vigentes; el ciudadano visualiza la lista completa y selecciona exactamente una credencial para la solicitud actual. Si necesita revocar otro credencial, deberá iniciar una nueva solicitud.

### 9.3.1. Existencia positiva y listado posterior vacío

El primer servicio confirma que existen credenciales y el ciudadano se autentica correctamente, pero el segundo servicio devuelve una lista vacía porque la disponibilidad cambió entre ambas consultas. El ciudadano no puede continuar, el sistema informa que actualmente no existen credenciales disponibles, no presenta el caso como error de autenticación y no inventa ninguna credencial.

### 9.4. Robo

El ciudadano sufre el robo del dispositivo o elemento relacionado con sus credenciales y decide revocar uno de las credenciales vigentes mediante la solicitud actual.

### 9.5. Pérdida

El ciudadano pierde el dispositivo o medio asociado y requiere revocar una credencial vinculado con esa situación.

### 9.6. Cambio de equipo o número

El ciudadano cambia de dispositivo, equipo o número relacionado con el servicio y considera necesario revocar una emisión anterior.

El efecto real del cambio de número sobre las credenciales debe confirmarse.

### 9.7. Sospecha de uso no autorizado

El ciudadano identifica señales de posible compromiso, acceso no reconocido o utilización indebida y decide revocar preventivamente una credencial vigente.

### 9.8. Otro motivo

La situación del ciudadano no corresponde a los motivos predefinidos. Selecciona la opción Otro e ingresa una descripción breve.

### 9.9. Autenticación no completada

El ciudadano no logra autenticarse, cancela el proceso de ID Perú o recibe un resultado negativo. La revocación no debe ejecutarse.

### 9.10. Ninguna credencial seleccionada

El ciudadano llega al paso de selección, pero no marca ninguna credencial. El sistema no permite continuar y solicita seleccionar al menos uno.

### 9.11. Revocación exitosa

La credencial seleccionada es revocada correctamente. El resultado es exitoso y la constancia refleja la credencial procesada.

### 9.12. Respuesta incompatible

Una respuesta que contenga resultados para varios UUID contradice el contrato de una solicitud con un único credencial. No debe normalizarse como resultado ciudadano: la integración debe rechazarla y tratarla como una incompatibilidad del proveedor que requiere validación operativa.

### 9.13. Revocación fallida

La credencial seleccionada no es revocada. El sistema no comunica éxito y muestra el resultado de la operación.

### 9.14. Resultado incierto

El servicio no puede confirmar el resultado de la credencial. El sistema no debe presentarla como revocada mientras no exista confirmación, no debe crear otra operación incompatible y debe reconciliar utilizando la misma clave de idempotencia.

### 9.15. Fallo de generación de constancia

La revocación fue procesada, pero la constancia no puede generarse o descargarse. El sistema debe conservar el resultado real de la revocación y tratar el problema documental como una incidencia separada.

### 9.16. Nuevo ingreso después de un trámite anterior

El ciudadano vuelve a la página de inicio e ingresa su DNI después de haber dejado incompleto o finalizado un trámite. Este ingreso representa una nueva intención de revocación: el sistema crea una solicitud diferente y consulta nuevamente las credenciales vigentes. No recupera el paso, la selección ni la constancia anterior.

Una solicitud anterior sin confirmar puede conservarse como abandonada. Una revocación confirmada todavía en curso o con resultado incierto puede bloquear temporalmente el nuevo inicio para evitar duplicidades, pero no debe abrir ni mostrar el trámite anterior.

---

## 10. Proceso actual

**Pendiente de validación.**

No se cuenta con información suficiente para describir cómo se realiza actualmente la revocación de credenciales digitales para personas naturales.

Debe confirmarse:

- Si existe actualmente un trámite equivalente.
- Si el procedimiento es presencial, digital o mixto.
- Qué áreas institucionales participan.
- Qué documentos o validaciones se solicitan.
- Cuánto tiempo demora.
- Si la revocación actual es inmediata.
- Si actualmente se permite elegir credenciales específicos.
- Cómo se identifican las diferentes emisiones.
- Cómo se entrega la constancia.
- Qué problemas concretos presenta el proceso vigente.
- Si el futuro sistema reemplazará, complementará o ampliará un canal existente.

No debe asumirse que el proceso actual es manual, presencial o ineficiente hasta contar con evidencia institucional.

---

## 11. Proceso esperado

El proceso funcional esperado está compuesto por una pantalla de inicio y cinco pasos.

### 11.1. Pantalla de inicio: ingreso del DNI y consulta

1. El ciudadano ingresa su número de DNI.
2. El sistema valida el formato básico.
3. El sistema consulta el primer servicio de existencia de credenciales.
4. El servicio devuelve un resultado positivo o negativo, sin lista ni datos individuales.
5. Si el resultado es negativo, el sistema informa que no existen credenciales disponibles y finaliza el proceso.
6. Si el resultado es positivo, el sistema permite continuar hacia ID Perú sin crear registros de credenciales.
7. Si el resultado es inconcluso, el servicio no está disponible, ocurre un timeout o existe un error técnico, el sistema bloquea la continuidad y permite un reintento seguro sin asumir ausencia.

### 11.2. Paso 1: autenticación

1. El ciudadano inicia la autenticación mediante ID Perú.
2. ID Perú devuelve el resultado de autenticación.
3. El sistema verifica que la identidad autenticada corresponda al DNI ingresado.
4. Una verificación exitosa conserva internamente el primer nombre autorizado por ID Perú y lo presenta solo dentro de la sesión autenticada, en la revisión del paso 4, el resultado del paso 5 y la constancia PDF.
5. Si la autenticación falla o se cancela, la revocación no se ejecuta.
6. Si la autenticación es correcta, queda pendiente la consulta del listado detallado.

### 11.3. Paso 2: credenciales vigentes

1. El sistema consume el segundo servicio y obtiene la lista detallada de credenciales digitales vigentes y revocadas.
2. Si la lista está vacía, informa que actualmente no existen credenciales disponibles y no permite continuar.
3. Si la lista contiene elementos, los conserva vinculados con la solicitud y muestra información comprensible para diferenciarlos:
   - Número de orden.
   - Fecha de creación.
   - Identificador o UUID, según la política de presentación.
4. El paso se muestra incluso cuando exista un solo credencial.
5. El ciudadano selecciona una credencial.
6. El sistema exige al menos una selección para continuar.

### 11.4. Paso 3: motivo

1. El ciudadano selecciona una causa predefinida o la opción Otro.
2. Cuando selecciona Otro, registra una descripción breve.
3. El sistema valida que exista un motivo válido.

### 11.5. Paso 4: confirmación

1. El sistema presenta un resumen de la operación.
2. El resumen incluye:
   - DNI parcialmente oculto.
   - Datos básicos del ciudadano, cuando corresponda.
   - Credencial seleccionado.
   - Motivo registrado.
   - Consecuencias de la revocación.
3. El ciudadano confirma expresamente la operación.
4. Solo después de la confirmación se envía el UUID seleccionado al servicio de revocación.

### 11.6. Paso 5: constancia

1. El servicio de revocación devuelve el resultado de la credencial confirmada.
2. El sistema clasifica el resultado como:
   - Exitoso.
   - Fallido.
   - Incierto.
3. El sistema presenta la credencial incluido y el resultado de la operación.
4. Se genera una constancia o comprobante coherente con ese resultado.
5. El ciudadano puede visualizarla o descargarla.

Flujo resumido:

```text
Pantalla de inicio
Ingreso del DNI y consulta de credenciales
        ↓
Paso 1
Autenticación mediante ID Perú
        ↓
Paso 2
Selección de credenciales digitales vigentes
        ↓
Paso 3
Selección del motivo
        ↓
Paso 4
Revisión y confirmación
        ↓
Ejecución de la revocación
        ↓
Paso 5
Resultado y constancia
```

---

## 12. Conceptos y terminología del dominio

| Término | Definición dentro del proyecto | Estado |
|---|---|---|
| RENIEC | Institución pública peruana responsable del servicio y del contexto de identificación y certificación digital | Confirmado |
| Ciudadano | Persona natural titular del DNI y de las credenciales digitales relacionados con la operación | Confirmado |
| DNI | Número de identificación ingresado para iniciar la consulta | Confirmado |
| DNI electrónico o DNIe | Documento de identidad electrónico relacionado con el uso de credenciales digitales. Su relación exacta con las credenciales incluidos debe precisarse | Preliminar |
| Credencial digital | Elemento digital individual que vincula la identidad del titular con mecanismos criptográficos y que puede invalidarse mediante revocación | Confirmado |
| Credencial digital vigente | Término principal utilizado en la interfaz para representar una credencial asociada al ciudadano que todavía puede ser revocada | Confirmado |
| Emisión vigente de credencial digital | Concepto que explica que la credencial fue generado en un momento determinado y constituye un elemento individual identificable | Confirmado |
| Lista de credenciales vigentes | Conjunto de elementos devueltos por el servicio de consulta para un DNI | Confirmado |
| Número de orden | Dato devuelto por el servicio para identificar o diferenciar una emisión dentro de la lista | Confirmado, significado exacto pendiente |
| Fecha de creación | Fecha asociada a la generación o creación de una emisión de credencial digital | Confirmado, formato pendiente |
| UUID | Identificador único utilizado para reconocer una credencial y solicitar su revocación | Confirmado |
| Selección de credenciales | Acción mediante la cual el ciudadano elige exactamente una credencial vigente | Confirmado |
| Revocación | Término recomendado para comunicar al ciudadano que la credencial dejará de ser válido | Confirmado |
| Revocación | Operación técnica mediante la cual una credencial digital deja de ser válido | Confirmado |
| Revocación inmediata | Característica por la cual la revocación se ejecuta después de la confirmación, sin evaluación administrativa posterior | Confirmado |
| Autenticación | Proceso utilizado para verificar que quien realiza la operación es el titular | Confirmado |
| ID Perú | Servicio externo utilizado para autenticar la identidad del ciudadano | Confirmado |
| Consulta de existencia de credenciales | Primera operación que confirma únicamente si el DNI tiene credenciales disponibles para revocar | Confirmado |
| Listado de credenciales vigentes | Segunda operación, posterior a la autenticación, que obtiene número de orden, fecha de creación y UUID | Confirmado |
| Motivo de revocación | Causa seleccionada por el ciudadano para registrar por qué realiza la operación | Confirmado |
| Otro motivo | Alternativa que permite ingresar una causa no contemplada en el catálogo | Confirmado |
| Confirmación | Manifestación expresa del ciudadano antes de ejecutar una operación inmediata e irreversible | Confirmado |
| Resultado de revocación | Estado devuelto para el único UUID confirmado en la solicitud | Confirmado |
| Resultado exitoso | La credencial seleccionada fue revocada correctamente | Confirmado |
| Resultado fallido | La credencial seleccionada no fue revocada correctamente | Confirmado |
| Resultado incierto | No se puede determinar con certeza el resultado final de la credencial seleccionada | Confirmado conceptualmente |
| Constancia o comprobante | Documento que acredita el resultado de la operación. Su nombre oficial y contenido están pendientes | Preliminar |
| Identidad digital | Concepto amplio que no debe utilizarse como objeto directo de la revocación | Confirmado como término no recomendado para la acción |
| Credencial digital | Término genérico insuficiente para identificar con precisión el objeto de la operación | Confirmado como término no recomendado |
| Sesión activa | Término no recomendado, porque representa una conexión temporal y no una emisión de credencial digital | Confirmado como término no aplicable |

---

## 13. Reglas de negocio identificadas

| Código | Regla | Estado |
|---|---|---|
| RN-01 | El servicio está dirigido a ciudadanos que actúan como personas naturales | Confirmado |
| RN-02 | El proceso se inicia mediante el ingreso del número de DNI | Confirmado |
| RN-03 | Antes de la autenticación se debe consultar únicamente la existencia de credenciales disponibles | Confirmado |
| RN-04 | El primer servicio debe devolver un resultado positivo o negativo y no una lista | Confirmado |
| RN-05 | El primer servicio no debe devolver cantidad, número de orden, fecha de creación ni UUID | Confirmado |
| RN-06 | Si el primer servicio confirma ausencia, el proceso no debe continuar hacia la autenticación | Confirmado |
| RN-07 | Solo un resultado positivo confirmado permite continuar hacia la autenticación; errores e incertidumbre no equivalen a ausencia | Confirmado |
| RN-08 | La autenticación del titular debe realizarse mediante ID Perú | Confirmado |
| RN-09 | La identidad autenticada debe corresponder al DNI ingresado | Preliminar, necesario para seguridad |
| RN-10 | Después de la autenticación, un segundo servicio debe obtener la lista completa con número de orden, fecha de creación, UUID, estado y fecha de revocación cuando corresponda | Confirmado |
| RN-11 | El paso de selección debe mostrarse siempre, incluso cuando exista un solo credencial | Confirmado |
| RN-12 | El ciudadano debe seleccionar exactamente una credencial por solicitud | Confirmado |
| RN-13 | Una solicitud sin credencial seleccionada no puede continuar | Confirmado |
| RN-14 | Cada credencial se identifica técnicamente mediante su UUID | Confirmado |
| RN-15 | La selección no se basará inicialmente en tipos de credencial | Confirmado |
| RN-16 | El ciudadano debe registrar un motivo antes de confirmar | Confirmado |
| RN-17 | Debe existir una opción Otro para situaciones no incluidas en el catálogo | Confirmado |
| RN-18 | Cuando se seleccione Otro, debe habilitarse un campo para describir el motivo | Confirmado |
| RN-19 | La obligatoriedad, longitud y validaciones del texto de Otro están pendientes | Pendiente de validación |
| RN-20 | La operación debe requerir una confirmación expresa | Confirmado |
| RN-21 | El resumen debe mostrar la credencial seleccionada | Confirmado |
| RN-22 | El servicio de revocación debe recibir el UUID del único credencial confirmada | Confirmado |
| RN-23 | Cada solicitud debe producir una sola operación técnica de revocación para su credencial confirmada | Confirmado |
| RN-24 | La operación general puede ser exitosa, fallida o incierta; no existe resultado parcial | Confirmado |
| RN-25 | El sistema solo debe comunicar éxito cuando el servicio confirme la credencial seleccionada | Confirmado |
| RN-26 | El resultado no puede ser parcial porque la solicitud contiene un único credencial | Confirmado |
| RN-27 | La constancia debe identificar la credencial seleccionada y reflejar su resultado | Confirmado |
| RN-28 | La revocación no implica revocar el DNI, la identidad civil ni la cuenta de ID Perú | Confirmado |
| RN-29 | La operación no emite una credencial nueva | Confirmado |
| RN-30 | El término revocación debe priorizarse en la interfaz y revocación en el contexto técnico | Confirmado |
| RN-31 | Las emisiones se presentarán al ciudadano como credenciales digitales vigentes | Confirmado |
| RN-32 | Cada ingreso del DNI desde la página de inicio representa una nueva solicitud y una nueva consulta de credenciales vigentes | Confirmado |
| RN-33 | El sistema no debe recuperar automáticamente pasos, selecciones, resultados ni constancias de una solicitud anterior | Confirmado |
| RN-34 | Las solicitudes anteriores deben conservarse como historial, sin convertirse en el contexto activo del nuevo ingreso | Confirmado |
| RN-35 | Una solicitud anterior no confirmada puede marcarse como abandonada cuando una nueva solicitud la sustituya | Confirmado |
| RN-36 | Una revocación confirmada en curso o con resultado incierto debe impedir temporalmente otro inicio sin revelar ni recuperar el trámite anterior | Confirmado por integridad e idempotencia |
| RN-37 | Un resultado inicial positivo crea una única sesión transaccional para la operación activa; home permanece pública y los pasos internos exigen esa sesión | Confirmado |
| RN-38 | ID Perú debe elevar la misma sesión existente y el cierre debe invalidarla, limpiar cookies y abandonar solo una solicitud reversible | Confirmado |
| RN-37 | La selección queda inmutable después de la confirmación ciudadana | Confirmado |
| RN-38 | Las credenciales no seleccionadas quedan fuera de la operación y no cambian de estado | Confirmado |
| RN-39 | Un resultado incierto conserva la misma operación y clave de idempotencia hasta su reconciliación | Confirmado |
| RN-40 | La consulta inicial no debe crear ni persistir registros de credenciales individuales | Confirmado |
| RN-41 | Si el primer servicio fue positivo y el listado posterior está vacío, el proceso se bloquea sin atribuir el resultado a una falla de autenticación | Confirmado |

---

## 14. Información y entidades principales

### 14.1. Ciudadano

Persona natural que inicia el proceso y debe acreditar que es el titular relacionado con el DNI consultado.

Información conceptual asociada:

- Número de DNI.
- Datos de identidad devueltos por ID Perú.
- Resultado de autenticación.
- Datos parcialmente ocultos para presentación.

### 14.2. DNI

Identificador utilizado para consultar las credenciales y relacionar la operación con el ciudadano.

No representa el objeto que será revocado.

### 14.3. Credencial digital vigente

Elemento individual asociado al ciudadano que puede ser seleccionado para revocación.

Información mínima confirmada:

- Número de orden.
- Fecha de creación.
- UUID.

Información adicional pendiente de validación:

- Estado exacto.
- Nombre o alias visible.
- Fecha de vencimiento.
- Emisor.
- Dispositivo o contexto asociado.
- Código de serie.
- Tipo de credencial.

### 14.4. Lista de credenciales digitales vigentes

Resultado del segundo servicio, consumido después de autenticar al ciudadano.

Posibles resultados conceptuales:

- Lista vacía.
- Lista con una credencial.
- Lista con varias credenciales.
- Error de consulta.
- Resultado no concluyente.

La lista debe mantenerse vinculada al proceso iniciado por el ciudadano y utilizarse en el paso de selección después de la autenticación.

### 14.5. Selección de credencial

Elección exclusiva de una credencial realizada por el ciudadano para la solicitud actual.

Información conceptual:

- UUID seleccionado.
- Fecha y hora de la selección.
- Relación con el proceso.
- Versión o referencia del listado detallado, si fuera necesaria.

### 14.6. Autenticación de identidad

Proceso realizado mediante ID Perú para acreditar al titular.

Debe registrar conceptualmente:

- Inicio de autenticación.
- Resultado.
- Identidad verificada.
- Revocación o error.
- Referencia de la operación, cuando corresponda.

### 14.7. Motivo de revocación

Causa declarada por el ciudadano.

Catálogo preliminar:

- Robo.
- Pérdida.
- Cambio de equipo o número.
- Sospecha de uso no autorizado.
- Otro motivo.

### 14.8. Descripción de otro motivo

Texto ingresado cuando las alternativas predefinidas no representan la situación del ciudadano.

### 14.9. Operación de revocación

Acción mediante la cual se solicita que la credencial seleccionada deje de ser válido.

Información conceptual:

- Ciudadano.
- DNI relacionado.
- Credencial seleccionado.
- UUID seleccionado.
- Motivo.
- Fecha y hora.
- Confirmación del ciudadano.
- Resultado general.
- Código de operación.
- Mensaje devuelto por el servicio.

### 14.10. Resultado de revocación

Respuesta correspondiente al único UUID confirmado en la solicitud.

Información conceptual:

- UUID solicitado.
- Estado del resultado.
- Código devuelto.
- Mensaje del servicio.
- Fecha y hora.
- Identificador de transacción, cuando corresponda.

### 14.11. Constancia o comprobante

Documento que acredita el resultado de la operación.

Su contenido exacto está pendiente de validación, pero preliminarmente podría incluir:

- Código de operación.
- DNI parcialmente oculto.
- Identificación parcial del titular.
- Fecha y hora.
- Motivo.
- Identificación de la credencial seleccionada.
- Identificación de cada credencial.
- Resultado común de la operación.
- Mecanismo de verificación.

### 14.12. Servicios externos

Componentes institucionales o externos necesarios para:

- Consultar credenciales.
- Autenticar al ciudadano.
- Ejecutar la revocación.
- Generar o verificar la constancia.

---

## 15. Estados y transiciones relevantes

### 15.1. Estado conceptual del proceso

| Estado | Descripción | Transición principal |
|---|---|---|
| Iniciado | El ciudadano accede al servicio | Ingreso del DNI |
| DNI ingresado | Se cuenta con un DNI con formato aceptable | Consulta de existencia |
| Consultando disponibilidad | Se está consultando si existe al menos una credencial disponible | Respuesta del primer servicio |
| Sin credenciales disponibles | El primer servicio confirmó ausencia | Finalización |
| Autenticación pendiente | El primer servicio confirmó existencia, pero todavía no existe lista detallada | Inicio de autenticación |
| En autenticación | El ciudadano se encuentra en ID Perú | Respuesta de ID Perú |
| Autenticación fallida o revocada | No se acreditó al titular | Reintento o finalización |
| Autenticado y listado pendiente | La identidad fue verificada, pero todavía debe ejecutarse el segundo servicio | Consulta detallada |
| Credenciales disponibles | El segundo servicio devolvió y el sistema persistió una lista válida con al menos una credencial vigente | Selección de credenciales |
| En selección | El ciudadano revisa las emisiones vigentes | Selección exclusiva de un elemento |
| Credencial seleccionado | Existe exactamente un UUID seleccionado | Registro del motivo |
| Motivo registrado | Se cuenta con una causa válida | Revisión y confirmación |
| Pendiente de confirmación | La operación está lista para ser autorizada | Confirmación o abandono |
| En revocación | El servicio está procesando el UUID seleccionado | Resultado del servicio |
| Exitosa | La credencial fue revocada | Generación de constancia |
| Fallida | La credencial no fue revocada | Presentación del resultado |
| Incierta | No puede determinarse el resultado de la credencial | Reconciliación sobre la misma operación |
| Constancia disponible | El comprobante puede consultarse o descargarse | Finalización |
| Constancia no disponible | Existe resultado de revocación, pero falló el documento | Reintento documental o soporte |

### 15.2. Estado conceptual de una credencial

| Estado | Descripción | Estado de definición |
|---|---|---|
| No consultado | El sistema todavía no lo conoce | Preliminar |
| Vigente y disponible | Puede mostrarse y seleccionarse para revocación | Confirmado conceptualmente |
| Seleccionado | Fue elegido por el ciudadano | Confirmado |
| No seleccionado | Se mantiene fuera de la operación actual | Confirmado |
| En proceso de revocación | Su UUID fue enviado al servicio | Confirmado conceptualmente |
| Revocado | El servicio confirmó que dejó de ser válido | Confirmado |
| Revocación fallida | El servicio confirmó que no pudo revocarse | Confirmado conceptualmente |
| Resultado incierto | No se conoce con certeza su estado final | Preliminar |
| Ya no disponible | Cambió de estado antes de procesarse | Pendiente de validación |

La clasificación exacta dependerá de los estados proporcionados por los servicios institucionales.

---

## 16. Supuestos preliminares

1. El primer servicio podrá confirmar si existe al menos una credencial disponible asociado a un DNI.

2. La respuesta funcional del primer servicio será positiva o negativa y no contendrá una lista.

3. Los errores, timeout, indisponibilidad y resultados inconclusos se diferenciarán de una respuesta negativa.

4. Después de la autenticación, el segundo servicio podrá devolver una lista completa con número de orden, fecha de creación, UUID, estado y fecha de revocación cuando corresponda.

5. La lista obtenida por el segundo servicio podrá mantenerse vinculada a la solicitud hasta el paso de selección.

6. ID Perú devolverá información suficiente para comprobar la identidad del titular.

7. Será posible relacionar la autenticación de ID Perú con el DNI ingresado.

8. El ciudadano podrá diferenciar las credenciales mediante la información mostrada.

9. El paso de selección se mostrará aun cuando exista un solo credencial.

10. El servicio de revocación aceptará el UUID seleccionado.

11. El servicio devolverá un único resultado para la credencial solicitado.

12. El resultado será exitoso, fallido o incierto; no existirá resultado parcial.

13. La revocación se ejecutará inmediatamente después de la confirmación.

14. La constancia identificará la credencial seleccionada y reflejará su resultado.

15. Los motivos mostrados representan un catálogo preliminar y pueden requerir ajustes.

16. La opción Otro requerirá una explicación.

17. La revocación será irreversible respecto de cada credencial revocada.

18. Para volver a utilizar funciones que dependan de una credencial revocada, el ciudadano podría necesitar una nueva emisión o activación. El proceso exacto está pendiente.

19. El portal será utilizado directamente por el ciudadano, sin intervención obligatoria de un operador.

20. Los servicios externos estarán disponibles para ser integrados, aunque sus contratos todavía no hayan sido proporcionados.

21. La constancia podrá descargarse en un formato documental. El formato definitivo está pendiente.

---

## 17. Restricciones y consideraciones

### 17.1. Institucionales

- La solución se desarrolla dentro del contexto de RENIEC.
- Los textos, términos y consecuencias deben ser aprobados institucionalmente.
- La operación debe alinearse con las políticas de certificación digital aplicables.
- Las responsabilidades entre áreas institucionales están pendientes de definición.
- La denominación de los datos devueltos por los servicios debe validarse con los responsables del dominio.

### 17.2. Seguridad

- La revocación es una operación sensible e irreversible.
- Debe impedirse su ejecución sin autenticación válida.
- Debe comprobarse la correspondencia entre el DNI consultado y la identidad autenticada.
- El UUID enviado debe pertenecer a la lista asociada al proceso.
- No debe permitirse inyectar UUID arbitrarios desde el cliente.
- Deben evitarse solicitudes duplicadas.
- El resultado debe ser trazable respecto de la credencial confirmada.
- El sistema no debe mostrar éxito sin confirmación del servicio.
- Debe analizarse el tratamiento de respuestas tardías o inciertas sobre la misma operación idempotente.
- La selección no puede alterarse después de la confirmación.
- Una respuesta con resultados para varios UUID debe rechazarse como incompatible con la solicitud singular.
- Debe evaluarse la revalidación del estado de las credenciales antes de ejecutar la revocación.
- El backend debe controlar la integridad de la selección y no confiar únicamente en el frontend.

### 17.3. Privacidad

- El sistema procesará información de identificación personal.
- Debe limitarse la exposición del DNI y de los datos personales.
- La consulta de existencia se realiza antes de la autenticación y no debe revelar cantidad ni información individual.
- La lista detallada solo debe obtenerse después de autenticar al ciudadano mediante el segundo servicio.
- Debe evaluarse si el UUID puede mostrarse completo o debe ocultarse parcialmente.
- El periodo de conservación de listas, motivos, evidencias y resultados está pendiente.
- Debe evitarse solicitar información confidencial dentro de Otro motivo.

### 17.4. Comunicación al ciudadano

- Debe utilizarse lenguaje comprensible.
- En la interfaz se empleará **credenciales digitales vigentes**.
- Cuando sea necesario explicar la coexistencia de varios elementos, se utilizará **emisiones vigentes de credenciales digitales**.
- No debe indicarse que se revoca la identidad del ciudadano.
- No debe afirmarse que se cancela el DNI.
- No deben comunicarse efectos que el servicio real no garantice.
- Debe diferenciarse entre revocar el trámite y revocar credenciales.
- Debe indicarse claramente que el ciudadano debe seleccionar exactamente uno.
- La consecuencia inmediata debe presentarse antes de la confirmación.
- El resultado debe comunicarse como exitoso, fallido o incierto para todo el conjunto; no debe sugerirse un éxito parcial.

### 17.5. Accesibilidad y usabilidad

- El flujo debe ser comprensible para ciudadanos con distintos niveles de conocimiento digital.
- La lista debe permitir diferenciar claramente cada credencial.
- La selección debe funcionar con teclado y tecnologías de asistencia.
- Los mensajes no deben depender únicamente del color.
- Debe existir una indicación clara de si ya existe una credencial seleccionada.
- Cuando exista un solo credencial, el paso debe seguir siendo comprensible y no parecer innecesario.
- Los criterios institucionales de accesibilidad están pendientes de validación.

### 17.6. Dependencias externas

El funcionamiento depende de:

- Servicio de consulta de credenciales.
- ID Perú.
- Servicio de revocación.
- Mecanismo de generación de constancias.

La indisponibilidad de cualquiera de estos servicios puede impedir completar el proceso.

### 17.7. Consideraciones legales y normativas

**Pendiente de validación.**

Debe identificarse:

- Marco normativo aplicable.
- Políticas de certificación.
- Requisitos de consentimiento.
- Reglas de conservación de evidencias.
- Valor de la constancia.
- Necesidad de términos y condiciones.
- Obligaciones de auditoría.
- Tratamiento de datos personales.
- Forma válida de acreditar la selección exclusiva de una credencial.
- Tratamiento legal de resultados inciertos y su reconciliación.

---

## 18. Información confirmada

- El sistema será desarrollado para RENIEC.
- El usuario principal será un ciudadano que actúa como persona natural.
- El objeto de la operación son las credenciales digitales vigentes.
- Las credenciales se encuentran asociados al DNI del ciudadano.
- Un ciudadano puede tener una o varias emisiones vigentes.
- Las diferentes emisiones pueden haber sido generadas en distintos momentos.
- En la interfaz se utilizará el término credenciales digitales vigentes.
- El término emisiones vigentes se utilizará para dar precisión conceptual.
- No se utilizará el término sesiones activas para representar estos credenciales.
- El ciudadano ingresará su número de DNI al inicio.
- El primer servicio web devolverá únicamente si existen o no credenciales disponibles para revocar.
- El primer servicio no devolverá lista, cantidad, número de orden, fecha de creación ni UUID.
- Una ausencia confirmada impedirá continuar; un resultado positivo permitirá avanzar hacia ID Perú.
- Un error, timeout, indisponibilidad o resultado inconcluso no se interpretará como ausencia.
- La consulta de existencia se realizará antes de la autenticación con ID Perú.
- ID Perú se utilizará para autenticar la identidad.
- ID Perú no es el objeto de la revocación.
- Después de autenticarse, un segundo servicio devolverá la lista completa con número de orden, fecha de creación, UUID, estado y fecha de revocación cuando corresponda.
- Después de obtener esa lista, el ciudadano verá el paso de credenciales vigentes.
- El paso de selección siempre se mostrará.
- Esto también se aplica cuando exista un solo credencial.
- El ciudadano podrá seleccionar exactamente una credencial.
- Debe existir exactamente una selección para continuar.
- El ciudadano deberá seleccionar un motivo.
- Se incluirá la opción Otro motivo.
- Al seleccionar Otro, podrá indicar una causa no contemplada.
- La confirmación mostrará la credencial seleccionada.
- El servicio de revocación recibirá el UUID seleccionado.
- La selección quedará inmutable después de la confirmación.
- El servicio procesará la credencial seleccionada y devolverá su resultado.
- La operación puede tener un resultado exitoso, fallido o incierto; no existe resultado parcial.
- Las credenciales no seleccionadas permanecerán fuera de la operación y no cambiarán de estado.
- La constancia deberá reflejar el resultado real.
- La operación será una revocación inmediata.
- Técnicamente, corresponde a una revocación.
- El sistema no cancela la identidad civil, el DNI ni la cuenta de ID Perú.

---

## 19. Aspectos pendientes de validación

### Prioridad alta

1. **Alcance exacto de las credenciales**
   Determinar qué credenciales están incluidos y cómo se relacionan con el DNIe.

2. **Criterio de vigencia y elegibilidad**
   Precisar qué estados hacen que una credencial aparezca en la lista.

3. **Contratos de los servicios de credenciales**
   Definir el contrato booleano del primer servicio y, por separado, la solicitud, lista, errores, códigos y límites del segundo servicio posterior a la autenticación.

4. **Semántica del número de orden**
   Confirmar si representa secuencia de emisión, posición de la lista u otro dato institucional.

5. **Fecha de creación**
   Confirmar su significado, zona horaria y formato.

6. **Uso y exposición del UUID**
   Definir si se muestra completo, parcial o mediante un identificador alternativo.

7. **Contrato del servicio de revocación**
   Confirmar que recibe un UUID y una clave de idempotencia, y que devuelve un resultado inequívoco.

8. **Resultado incierto y reconciliación**
   Precisar códigos, mensajes y consultas permitidas sobre la misma operación idempotente.

9. **Correspondencia entre DNI e identidad autenticada**
   Confirmar cómo se validará técnicamente.

10. **Cambios de estado durante el flujo**
    Precisar reintentos y soporte cuando el primer servicio fue positivo y el segundo servicio devuelve una lista vacía; funcionalmente el proceso se bloquea sin tratarlo como error de autenticación.

11. **Efectos exactos de la revocación**
    Determinar qué funciones dejarán de estar disponibles.

12. **Idempotencia y duplicidad**
    Definir el comportamiento ante reintentos o envíos repetidos.

### Prioridad media

13. **Información visible en el paso de selección**
    Validar textos, orden, ocultamiento y datos complementarios.

14. **Presentación de la selección exclusiva**
    Validar institucionalmente el control visual utilizado para elegir y reemplazar un único credencial antes de continuar.

15. **Catálogo definitivo de motivos**
    Validar nombres, descripciones y códigos institucionales.

16. **Reglas de Otro motivo**
    Determinar obligatoriedad, longitud, caracteres permitidos y tratamiento de información sensible.

17. **Contenido de la confirmación**
    Aprobar textos legales, advertencias y consecuencias.

18. **Constancia o comprobante**
    Definir nombre oficial, campos, formato, mecanismo de verificación y valor institucional.

19. **Constancia para la credencial confirmada**
    Determinar cómo documentar la credencial seleccionada y su resultado exitoso, fallido o incierto.

20. **Proceso actual**
    Documentar el procedimiento vigente y los problemas que se espera resolver.

21. **Reintentos**
    Establecer cuándo puede repetirse una consulta, autenticación o revocación.

22. **Canales de soporte**
    Definir la derivación para casos no resueltos.

### Prioridad complementaria

23. Reglas de auditoría y trazabilidad.

24. Conservación de datos y evidencias.

25. Políticas de privacidad.

26. Requisitos de accesibilidad.

27. Disponibilidad esperada del servicio.

28. Responsables institucionales del proceso.

29. Mensajes para credenciales vencidas o previamente revocadas.

30. Comportamiento cuando falla la generación de la constancia.

31. Ordenamiento de la lista de credenciales.

32. Paginación o cantidad máxima de credenciales, si fuera aplicable.

33. Revalidación de la lista antes de confirmar.

---

## 20. Riesgos de interpretación

### 20.1. Confundir credenciales digitales con identidad digital

El sistema no elimina ni revoca la identidad de la persona. La identidad digital no debe presentarse como objeto de la operación.

### 20.2. Confundir revocación con revocación

Ambos términos representan perspectivas distintas:

- Revocación: comunicación ciudadana.
- Revocación: operación técnica.

### 20.3. Confundir DNI con credenciales digitales

El DNI se utiliza para identificar al ciudadano e iniciar la consulta. No es el elemento revocado.

### 20.4. Confundir DNI con DNI electrónico

El sistema solicita el número de DNI, pero las credenciales pueden estar relacionados con el contexto del DNIe. La relación exacta debe documentarse.

### 20.5. Confundir varias emisiones con varios tipos de credencial

La existencia de varias credenciales vigentes no implica necesariamente que existan distintas clases o tipos. Pueden ser emisiones generadas en momentos diferentes.

### 20.6. Utilizar el término sesiones activas

Una sesión es una conexión temporal. No representa adecuadamente una emisión de credencial digital y podría llevar a pensar que la operación equivale a cerrar una sesión.

### 20.7. Confundir emisión con duplicado

Cada emisión corresponde a una credencial individual con UUID propio. No debe comunicarse necesariamente como una copia del mismo archivo o credencial.

### 20.8. Suponer selección automática cuando existe un solo credencial

Aunque exista un único elemento, el paso de selección siempre debe mostrarse y requerir una acción expresa del ciudadano.

### 20.9. Interpretar que se cancelan todos las credenciales

El ciudadano selecciona exactamente uno. Los no seleccionados quedan fuera de la operación actual.

### 20.10. Aceptar resultados para varios UUID

Una respuesta con varios resultados contradice la regla de una credencial por solicitud. Debe rechazarse como contrato incompatible y nunca comunicarse como éxito parcial.

### 20.11. Interpretar ID Perú como la credencial revocada

ID Perú se utiliza para autenticar al titular. La operación no cancela ID Perú.

### 20.12. Interpretar la revocación como una solicitud diferida

La operación está planteada como inmediata después de la confirmación.

### 20.13. Afirmar efectos no confirmados

Revocar credenciales no necesariamente implica:

- Eliminar una aplicación.
- Retirar automáticamente un DNI digital.
- Cerrar todas las sesiones.
- Bloquear todos los servicios del Estado.
- Desvincular un dispositivo.
- Revocar la identidad del ciudadano.

### 20.14. Confundir ausencia de credenciales con credenciales ya revocadas

El servicio podría distinguir varios estados. No debe asumirse que todos equivalen a “no tiene credenciales”.

### 20.15. Confundir constancia generada con revocación ejecutada

La revocación y la generación del comprobante son resultados relacionados, pero conceptualmente distintos.

---

## 21. Límites del documento

Este documento consolida el contexto funcional y conceptual disponible. No define todavía:

- Requisitos funcionales detallados.
- Requisitos no funcionales completos.
- Casos de uso formales.
- Historias de usuario.
- Criterios de aceptación completos.
- Arquitectura técnica.
- Lenguajes o frameworks.
- Diseño de base de datos.
- Modelos físicos de datos.
- Contratos definitivos de API.
- Firmas definitivas de servicios web.
- Diagramas técnicos de integración.
- Políticas detalladas de seguridad.
- Modelo completo de auditoría.
- Plan de pruebas.
- Plan de implementación.
- Plan de desarrollo.
- Estimaciones.
- Diseño visual definitivo.
- Manuales de usuario.
- Procedimiento operativo institucional.
- Análisis jurídico o normativo definitivo.

Las definiciones aquí incluidas deben revisarse cuando RENIEC proporcione documentación institucional, reglas operativas, contratos de servicios y criterios normativos adicionales.

---

## 22. Resumen reutilizable para otras herramientas

El proyecto consiste en un sistema web institucional para RENIEC, dirigido a personas naturales, que permite revocar de manera inmediata una credencial digital vigente asociado a un DNI por solicitud.

Un ciudadano puede tener una o más emisiones vigentes de credenciales digitales generadas en momentos diferentes. En la interfaz, estas emisiones se presentan como credenciales digitales vigentes. Cada credencial se identifica mediante un número de orden, una fecha de creación y un UUID único.

El flujo comienza en una pantalla de inicio donde el ciudadano ingresa su DNI. Un primer servicio web indica únicamente si existen credenciales disponibles. Si confirma ausencia, el proceso finaliza; si confirma existencia, el ciudadano continúa con la autenticación mediante ID Perú. Los errores o resultados inciertos bloquean la continuidad sin interpretarse como ausencia.

Después de autenticarse, un segundo servicio obtiene la lista con número de orden, fecha de creación y UUID. Solo entonces se persisten y muestran las credenciales en el paso de selección. Si la lista posterior está vacía, el proceso se bloquea sin considerarlo un error de autenticación. El paso siempre se presenta cuando existe al menos una credencial, incluso si solo hay uno, y el ciudadano debe seleccionar exactamente uno para continuar.

Luego selecciona el motivo de revocación, revisa un resumen y confirma expresamente la operación. La selección queda inmutable. El sistema envía al servicio de revocación el UUID seleccionado bajo una única clave de idempotencia. El resultado es exitoso si lo revoca, fallido si no lo revoca o incierto mientras no pueda confirmarse uno de esos resultados.

Finalmente, el sistema muestra la credencial procesada y su resultado, y genera una constancia coherente. Las credenciales no seleccionadas permanecen fuera de la operación.

Cada ingreso posterior del DNI desde la página de inicio inicia una solicitud nueva y una consulta actualizada. El progreso, la selección y la constancia de solicitudes anteriores no se recuperan automáticamente. El historial se conserva para evidencia y trazabilidad; una revocación todavía en curso o incierta solo puede bloquear temporalmente un nuevo inicio para evitar operaciones duplicadas.

En la comunicación ciudadana se utilizará revocación de credenciales digitales; técnicamente la operación corresponde a una revocación. El sistema no cancela la identidad civil, el DNI, el DNIe ni la cuenta de ID Perú. Tampoco debe utilizarse el término sesiones activas para representar las credenciales.

Permanecen pendientes los contratos definitivos de los servicios, los estados exactos, la presentación del UUID, los efectos de la revocación, el contenido de la constancia, las reglas de auditoría, privacidad y normativa aplicable.

---

## 23. Contexto compacto para prompts

> Proyecto web de RENIEC para que personas naturales cancelen inmediatamente una credencial digital vigente asociado a su DNI por solicitud. El ciudadano ingresa su DNI y un primer servicio indica únicamente si existen credenciales disponibles, sin devolver lista, cantidad ni datos individuales. Solo un resultado positivo permite continuar hacia ID Perú; los errores y la incertidumbre no equivalen a ausencia. Después de autenticar al ciudadano, un segundo servicio devuelve la lista con número de orden, fecha de creación y UUID. Si está vacía, el proceso se bloquea; si contiene elementos, se persisten y siempre se muestra el paso de selección, incluso cuando exista uno solo. El ciudadano elige exactamente una credencial, registra el motivo, revisa y confirma; desde ese momento la selección es inmutable. El servicio de revocación recibirá ese UUID bajo una clave de idempotencia. La operación puede ser exitosa, fallida o incierta, y la constancia identifica la credencial y su resultado. Cada ingreso posterior desde inicio crea una solicitud nueva y no recupera progreso anterior. En la interfaz se usa “revocación de credenciales digitales”; técnicamente es una revocación. No se cancela la identidad civil, el DNI, el DNIe ni ID Perú.
