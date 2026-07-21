# Contexto del proyecto

## 1. Resumen ejecutivo

El proyecto consiste en el diseño e implementación de un sistema web institucional para el **Registro Nacional de Identificación y Estado Civil (RENIEC) del Perú**, dirigido a ciudadanos que actúan como personas naturales.

Su propósito es proporcionar un canal digital de autoservicio mediante el cual el titular pueda **cancelar de forma inmediata uno o varios certificados digitales vigentes asociados a su DNI**, cuando exista una situación que pueda comprometer su seguridad, como pérdida, robo, cambio de equipo o sospecha de uso no autorizado.

En la comunicación dirigida al ciudadano se utilizará preferentemente el término **cancelación de certificados digitales**. Desde la perspectiva técnica y del dominio de certificación digital, la operación ejecutada corresponde a una **revocación**.

Un ciudadano puede tener una o más **emisiones vigentes de certificados digitales** asociadas a su DNI. Cada emisión corresponde a un certificado generado en un momento determinado y puede identificarse mediante un número de orden, una fecha de creación y un UUID único. En la interfaz ciudadana, estas emisiones se presentarán como **certificados digitales vigentes**.

El sistema no cancela la identidad civil, el número de DNI, el documento físico, el DNI electrónico ni la cuenta de ID Perú. Su objeto funcional son los certificados digitales vigentes seleccionados expresamente por el ciudadano.

El flujo general comprende una pantalla de inicio y cinco pasos:

1. Ingreso del número de DNI y consulta de certificados digitales vigentes.
2. Autenticación del titular mediante ID Perú.
3. Selección de uno o varios certificados digitales vigentes.
4. Selección del motivo de cancelación.
5. Confirmación informada de la operación.
6. Ejecución inmediata de la revocación.
7. Presentación del resultado y generación de una constancia o comprobante.

El sistema dependerá de servicios externos para consultar los certificados digitales vigentes, autenticar al ciudadano y ejecutar la revocación. El servicio de consulta devolverá una lista de certificados. El servicio de revocación recibirá en una sola operación la lista completa de UUID confirmados y deberá aplicar una regla atómica: cancelar todos los certificados seleccionados o no cancelar ninguno.

---

## 2. Antecedentes y contexto

RENIEC es la institución pública responsable de la identificación de las personas y del registro de los hechos relativos al estado civil en el Perú. Dentro de su ámbito de identidad y certificación digital, los ciudadanos pueden contar con certificados digitales vinculados a su identificación y al uso de servicios digitales.

Los certificados digitales pueden utilizarse para acreditar identidad en entornos digitales, realizar operaciones de autenticación y ejecutar funciones relacionadas con la firma digital. Mientras permanecen vigentes, pueden ser aceptados por sistemas y servicios que confían en ellos.

Un mismo ciudadano puede contar con varias emisiones vigentes de certificados digitales generadas en diferentes momentos. Esto no implica necesariamente que existan diferentes tipos de certificado, sino que pueden coexistir varios certificados vigentes asociados al mismo titular, cada uno con su propia fecha de creación y su propio identificador único.

Determinadas situaciones pueden hacer necesario que el titular impida la utilización de uno o varios de estos certificados, especialmente cuando:

- Pierde el dispositivo o elemento relacionado con su uso.
- Sufre el robo de dicho dispositivo.
- Cambia de equipo o número asociado.
- Sospecha que alguno de sus mecanismos digitales fue comprometido.
- Identifica una emisión que ya no desea mantener vigente.
- Presenta otra circunstancia que justifica la cancelación.

El proyecto busca establecer un canal remoto, comprensible y seguro para que una persona natural pueda consultar la existencia de certificados digitales vigentes, autenticar su identidad, seleccionar cuáles desea cancelar y obtener evidencia del resultado de la operación.

La base normativa específica, las políticas institucionales aplicables, las responsabilidades operativas y el procedimiento vigente de cancelación todavía deben ser documentados y validados formalmente.

---

## 3. Definición del problema

### 3.1. Situación identificada

Un ciudadano puede tener uno o varios certificados digitales vigentes asociados a su DNI, generados en momentos distintos. Estos certificados pueden continuar siendo válidos aun cuando se produzca una situación de riesgo, como pérdida, robo, cambio de equipo o sospecha de acceso no autorizado.

En estos casos, el titular necesita contar con un mecanismo que le permita identificar las emisiones vigentes asociadas a su DNI y cancelar únicamente aquellas que considere comprometidas o que ya no desee mantener activas.

No se dispone todavía de información confirmada sobre la forma en que este procedimiento se realiza actualmente, si existe un canal digital previo, si requiere atención presencial o si depende de intervención administrativa.

### 3.2. Dificultades preliminares

A partir de la información disponible, se identifican las siguientes dificultades:

- El ciudadano podría no contar con un canal digital directo para cancelar sus certificados.
- La demora en la revocación podría prolongar el periodo de exposición ante un posible uso no autorizado.
- Los conceptos de identidad digital, DNI electrónico, credencial digital, emisión y certificado digital pueden resultar confusos.
- El titular no necesariamente conoce cuántas emisiones vigentes tiene asociadas a su DNI.
- El ciudadano necesita distinguir los certificados por datos simples, sin depender de detalles criptográficos o técnicos.
- La operación requiere consultar previamente los certificados susceptibles de cancelación.
- La cancelación debe ser autorizada exclusivamente por el titular.
- El sistema debe permitir seleccionar uno o varios certificados.
- La operación debe producir un único resultado común para el conjunto confirmado.
- El ciudadano necesita recibir evidencia clara del conjunto seleccionado y de su resultado común.

### 3.3. Causas preliminares

Las causas del problema todavía requieren análisis formal. Como hipótesis iniciales se consideran:

- Ausencia o insuficiencia de un canal de autoservicio especializado.
- Dependencia de procesos operativos que podrían no ser inmediatos.
- Complejidad técnica del dominio de certificación digital.
- Falta de claridad sobre la coexistencia de varias emisiones vigentes.
- Necesidad de integrar servicios diferentes para consultar, autenticar y revocar.
- Necesidad de preservar la atomicidad, la idempotencia y el tratamiento seguro de resultados inciertos.

### 3.4. Consecuencias posibles

Mientras uno o varios certificados permanezcan vigentes después de una situación de riesgo, podrían producirse:

- Intentos de autenticación no autorizada.
- Uso indebido de mecanismos de identificación digital.
- Riesgo de operaciones realizadas sin autorización del titular.
- Incertidumbre sobre cuáles certificados continúan vigentes.
- Cancelación innecesaria de certificados que el ciudadano todavía desea conservar.
- Dificultad para acreditar qué certificados fueron revocados.
- Pérdida de confianza en los servicios digitales institucionales.

Estas consecuencias son interpretaciones derivadas del propósito del proyecto y deberán alinearse con el alcance real de los certificados involucrados.

### 3.5. Necesidad que justifica el sistema

Se requiere un servicio web que permita al ciudadano:

- Ingresar su número de DNI.
- Consultar si cuenta con certificados digitales vigentes susceptibles de cancelación.
- Impedir la continuidad cuando no exista ningún certificado disponible.
- Demostrar que es el titular mediante ID Perú.
- Visualizar las emisiones vigentes mediante información comprensible.
- Seleccionar uno o varios certificados digitales vigentes.
- Registrar el motivo de la operación.
- Comprender las consecuencias antes de confirmar.
- Ejecutar la revocación de los certificados seleccionados.
- Consultar el resultado común de la operación confirmada.
- Obtener una constancia verificable del resultado.

---

## 4. Objetivo general del proyecto

Implementar un servicio web institucional de RENIEC que permita a una persona natural cancelar de manera segura e inmediata uno o varios certificados digitales vigentes asociados a su DNI, previa consulta de las emisiones disponibles, autenticación del titular mediante ID Perú, selección expresa de los certificados, registro del motivo y confirmación de la operación.

---

## 5. Objetivos específicos preliminares

- Permitir que el ciudadano inicie el proceso utilizando su número de DNI.
- Consultar mediante un servicio institucional las emisiones vigentes de certificados digitales asociadas al DNI.
- Recibir y conservar temporalmente la lista de certificados obtenida durante la consulta inicial.
- Evitar que el proceso continúe cuando la lista devuelta esté vacía.
- Autenticar al titular mediante el servicio externo ID Perú.
- Verificar la correspondencia entre la identidad autenticada y el DNI ingresado.
- Mostrar los certificados digitales vigentes después de la autenticación.
- Permitir seleccionar uno o varios certificados.
- Mostrar siempre el paso de selección, incluso cuando exista un solo certificado.
- Exigir la selección de al menos un certificado antes de continuar.
- Registrar el motivo de la cancelación.
- Incorporar una opción abierta para motivos no contemplados en el catálogo.
- Informar al ciudadano sobre el carácter inmediato y las consecuencias de la operación.
- Obtener una confirmación explícita antes de ejecutar la revocación.
- Enviar al servicio de revocación la lista de UUID seleccionados.
- Recibir un único resultado atómico para toda la lista procesada.
- Comunicar un resultado exitoso, fallido o incierto, sin estados parciales.
- Generar una constancia o comprobante coherente con el resultado real.

---

## 6. Descripción general de la solución

La solución propuesta es un **portal web de cancelación de certificados digitales vigentes para personas naturales**.

En la pantalla de inicio, el ciudadano ingresará su número de DNI. El sistema consultará un servicio externo que devolverá una lista de certificados digitales vigentes susceptibles de cancelación. Cada elemento de la lista incluirá, como mínimo:

- Número de orden.
- Fecha de creación.
- UUID o identificador único.

Si la lista está vacía, el sistema informará al ciudadano que no existen certificados disponibles para cancelar y no permitirá continuar. Si existe al menos un certificado, el sistema conservará la lista dentro del proceso y permitirá iniciar la autenticación.

Después de autenticar su identidad mediante ID Perú, el ciudadano accederá al paso de selección de certificados. Este paso se mostrará siempre, incluso cuando la lista contenga un solo elemento. El usuario deberá seleccionar al menos un certificado para continuar.

Posteriormente, el titular seleccionará el motivo de la cancelación. El sistema ofrecerá un conjunto de motivos predefinidos y una alternativa denominada **Otro motivo**, que permitirá registrar una explicación adicional.

Antes de ejecutar la operación, el sistema presentará un resumen que incluirá los certificados seleccionados, el motivo registrado y las advertencias correspondientes. La revocación solo se solicitará después de la confirmación expresa del ciudadano. Desde ese momento, la selección queda inmutable.

El servicio de revocación recibirá la lista completa de UUID seleccionados bajo una única clave de idempotencia. El proveedor deberá garantizar semántica atómica para la lista: un éxito confirma la revocación de todos los seleccionados; un fallo confirma que ninguno fue revocado; un resultado incierto no permite afirmar ninguno de los dos resultados y deberá reconciliarse sobre la misma operación. Un proveedor que solo permita resultados independientes o parciales es incompatible con esta regla.

Cuando la operación concluya, el sistema mostrará el conjunto de certificados procesados y su resultado común, y pondrá a disposición una constancia o comprobante coherente con dicho resultado.

---

## 7. Alcance funcional preliminar

### 7.1. Procesos incluidos

El alcance preliminar comprende:

- Ingreso y validación básica del número de DNI.
- Consulta de certificados digitales vigentes.
- Recepción de una lista de certificados.
- Evaluación de continuidad según el tamaño de la lista.
- Conservación temporal de la lista dentro del proceso.
- Autenticación mediante ID Perú.
- Validación de correspondencia entre el DNI y la identidad autenticada.
- Visualización de certificados digitales vigentes.
- Selección de uno o varios certificados.
- Validación de selección mínima.
- Registro del motivo de cancelación.
- Registro de un motivo personalizado mediante la opción Otro.
- Presentación de advertencias y consecuencias.
- Confirmación expresa de la cancelación.
- Envío de una lista de UUID al servicio de revocación.
- Recepción de un resultado atómico para el conjunto seleccionado.
- Determinación del resultado general de la operación.
- Presentación del resultado.
- Generación y descarga de una constancia o comprobante.
- Manejo general de respuestas negativas, errores y falta de disponibilidad.

### 7.2. Acciones generales disponibles para el ciudadano

- Ingresar su DNI.
- Continuar o abandonar el proceso.
- Autenticarse mediante ID Perú.
- Revisar los certificados digitales vigentes.
- Seleccionar uno o varios certificados.
- Desmarcar certificados antes de confirmar.
- Seleccionar un motivo.
- Escribir un motivo alternativo.
- Revisar la información antes de confirmar.
- Confirmar la cancelación.
- Consultar el resultado común de la operación.
- Descargar una constancia.
- Finalizar el proceso.

### 7.3. Elementos fuera del alcance preliminar

No forman parte del alcance confirmado:

- Emisión de nuevos certificados digitales.
- Renovación de certificados digitales.
- Reactivación de certificados revocados.
- Cancelación del número de DNI.
- Cancelación o bloqueo del documento físico.
- Eliminación de la identidad civil del ciudadano.
- Cancelación de la cuenta de ID Perú.
- Administración general de identidades digitales.
- Gestión de usuarios institucionales.
- Módulos administrativos o de atención interna.
- Recuperación de credenciales.
- Gestión general de dispositivos.
- Cierre de sesiones en otros sistemas.
- Bloqueo general de todos los servicios digitales del ciudadano.
- Modificación de los datos técnicos de un certificado.
- Consulta pública de certificados de terceros.
- Selección basada en tipos de certificado, salvo que posteriormente se confirme esa necesidad.

### 7.4. Aspectos del alcance pendientes de confirmación

- Tipo exacto de certificados incluidos.
- Criterio utilizado para considerar un certificado vigente y cancelable.
- Tratamiento de certificados vencidos, suspendidos o previamente revocados.
- Nombre y significado exacto del número de orden.
- Formato exacto de la fecha de creación.
- Datos adicionales que podría devolver el servicio de consulta.
- Si el UUID debe mostrarse completo, parcialmente oculto o mediante un identificador amigable.
- Revalidación de la lista después de la autenticación.
- Posibilidad de que un certificado cambie de estado durante el proceso.
- Alcance exacto de los efectos posteriores.
- Contenido y validez de la constancia.
- Existencia de canales de soporte o derivación.
- Reglas para reintentos y resultados inciertos.

---

## 8. Usuarios y actores involucrados

| Actor | Rol dentro del proceso | Necesidad principal | Acciones o responsabilidades | Situaciones de participación |
|---|---|---|---|---|
| Ciudadano o persona natural | Usuario principal y titular de los certificados digitales | Cancelar oportunamente uno o varios certificados vigentes | Ingresar DNI, autenticarse, seleccionar certificados, registrar motivo, confirmar y descargar la constancia | Robo, pérdida, cambio de equipo o número, sospecha de uso no autorizado u otro motivo |
| RENIEC | Institución responsable del servicio | Proporcionar un canal confiable y trazable | Administrar el servicio, definir reglas, comunicar resultados y garantizar la trazabilidad institucional | Durante todo el proceso |
| ID Perú | Servicio externo de autenticación | Confirmar que la persona que realiza el proceso es quien afirma ser | Autenticar al ciudadano y devolver el resultado correspondiente | Después de verificar que existe al menos un certificado |
| Servicio de consulta de certificados | Sistema externo colaborador | Obtener las emisiones vigentes asociadas al DNI | Recibir el DNI y devolver una lista de certificados con número de orden, fecha de creación y UUID | Desde la pantalla de inicio |
| Servicio de revocación | Sistema externo colaborador | Ejecutar de forma atómica la revocación de los certificados seleccionados | Recibir la lista completa de UUID y una clave de idempotencia, y devolver un único resultado para el conjunto | Después de la confirmación |
| Servicio o mecanismo de constancias | Actor conceptual pendiente de validación | Generar evidencia de la operación | Producir o proporcionar el comprobante correspondiente | Después de contar con el resultado de la revocación |
| Personal de soporte o atención | Actor potencial pendiente de validación | Atender incidencias o casos no resueltos en línea | Orientar, revisar incidencias o derivar casos | Errores, indisponibilidad o resultados inciertos |

---

## 9. Escenarios principales de uso

### 9.1. DNI sin certificados digitales vigentes

El ciudadano ingresa su DNI, pero el servicio devuelve una lista vacía. El sistema informa que no existen certificados disponibles para cancelar y finaliza el proceso sin solicitar autenticación.

### 9.2. DNI con un solo certificado vigente

El servicio devuelve un único certificado. El ciudadano se autentica y accede igualmente al paso de selección. El certificado no debe omitirse ni cancelarse automáticamente; el usuario debe seleccionarlo de forma expresa.

### 9.3. DNI con varios certificados vigentes

El servicio devuelve varias emisiones vigentes. Después de autenticarse, el ciudadano visualiza la lista y puede seleccionar uno, varios o todos los certificados disponibles.

### 9.4. Robo

El ciudadano sufre el robo del dispositivo o elemento relacionado con el uso de uno o varios certificados y decide cancelarlos.

### 9.5. Pérdida

El ciudadano pierde el dispositivo o medio asociado y requiere cancelar los certificados vinculados con esa situación.

### 9.6. Cambio de equipo o número

El ciudadano cambia de dispositivo, equipo o número relacionado con el servicio y considera necesario cancelar una o varias emisiones anteriores.

El efecto real del cambio de número sobre los certificados debe confirmarse.

### 9.7. Sospecha de uso no autorizado

El ciudadano identifica señales de posible compromiso, acceso no reconocido o utilización indebida y decide cancelar preventivamente los certificados correspondientes.

### 9.8. Otro motivo

La situación del ciudadano no corresponde a los motivos predefinidos. Selecciona la opción Otro e ingresa una descripción breve.

### 9.9. Autenticación no completada

El ciudadano no logra autenticarse, cancela el proceso de ID Perú o recibe un resultado negativo. La revocación no debe ejecutarse.

### 9.10. Ningún certificado seleccionado

El ciudadano llega al paso de selección, pero no marca ningún certificado. El sistema no permite continuar y solicita seleccionar al menos uno.

### 9.11. Revocación exitosa

Todos los certificados seleccionados son cancelados correctamente. El resultado general es exitoso y la constancia refleja el conjunto procesado y el resultado común.

### 9.12. Respuesta mixta incompatible

Una respuesta que afirme éxito para algunos UUID y fallo para otros contradice la regla atómica. No debe normalizarse como resultado ciudadano: la integración debe rechazarla y tratarla como una incompatibilidad del proveedor que requiere validación operativa.

### 9.13. Revocación fallida

Ninguno de los certificados seleccionados es cancelado. El sistema no comunica éxito y muestra el resultado común de la operación.

### 9.14. Resultado incierto

El servicio no puede confirmar el resultado atómico del conjunto. El sistema no debe presentar ningún certificado seleccionado como cancelado mientras no exista confirmación, no debe crear una operación incompatible y debe reconciliar utilizando la misma clave de idempotencia.

### 9.15. Fallo de generación de constancia

La revocación fue procesada, pero la constancia no puede generarse o descargarse. El sistema debe conservar el resultado real de la revocación y tratar el problema documental como una incidencia separada.

### 9.16. Nuevo ingreso después de un trámite anterior

El ciudadano vuelve a la página de inicio e ingresa su DNI después de haber dejado incompleto o finalizado un trámite. Este ingreso representa una nueva intención de cancelación: el sistema crea una solicitud diferente y consulta nuevamente los certificados vigentes. No recupera el paso, la selección ni la constancia anterior.

Una solicitud anterior sin confirmar puede conservarse como abandonada. Una revocación confirmada todavía en curso o con resultado incierto puede bloquear temporalmente el nuevo inicio para evitar duplicidades, pero no debe abrir ni mostrar el trámite anterior.

---

## 10. Proceso actual

**Pendiente de validación.**

No se cuenta con información suficiente para describir cómo se realiza actualmente la cancelación de certificados digitales para personas naturales.

Debe confirmarse:

- Si existe actualmente un trámite equivalente.
- Si el procedimiento es presencial, digital o mixto.
- Qué áreas institucionales participan.
- Qué documentos o validaciones se solicitan.
- Cuánto tiempo demora.
- Si la cancelación actual es inmediata.
- Si actualmente se permite elegir certificados específicos.
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
3. El sistema consulta el servicio de certificados.
4. El servicio devuelve una lista de emisiones vigentes.
5. Si la lista está vacía, el sistema informa que no existen certificados disponibles y finaliza el proceso.
6. Si existe al menos un elemento, el sistema conserva la lista y permite continuar.

### 11.2. Paso 1: autenticación

1. El ciudadano inicia la autenticación mediante ID Perú.
2. ID Perú devuelve el resultado de autenticación.
3. El sistema verifica que la identidad autenticada corresponda al DNI ingresado.
4. Si la autenticación falla o se cancela, la revocación no se ejecuta.
5. Si la autenticación es correcta, se habilita el paso de selección.

### 11.3. Paso 2: certificados vigentes

1. El sistema muestra los certificados digitales vigentes obtenidos durante la consulta inicial.
2. Cada elemento muestra información comprensible para diferenciarlo:
   - Número de orden.
   - Fecha de creación.
   - Identificador o UUID, según la política de presentación.
3. El paso se muestra incluso cuando exista un solo certificado.
4. El ciudadano selecciona uno o varios certificados.
5. El sistema exige al menos una selección para continuar.

### 11.4. Paso 3: motivo

1. El ciudadano selecciona una causa predefinida o la opción Otro.
2. Cuando selecciona Otro, registra una descripción breve.
3. El sistema valida que exista un motivo válido.

### 11.5. Paso 4: confirmación

1. El sistema presenta un resumen de la operación.
2. El resumen incluye:
   - DNI parcialmente oculto.
   - Datos básicos del ciudadano, cuando corresponda.
   - Certificados seleccionados.
   - Motivo registrado.
   - Consecuencias de la cancelación.
3. El ciudadano confirma expresamente la operación.
4. Solo después de la confirmación se envían los UUID al servicio de revocación.

### 11.6. Paso 5: constancia

1. El servicio de revocación devuelve un único resultado para el conjunto confirmado.
2. El sistema clasifica el resultado como:
   - Exitoso.
   - Fallido.
   - Incierto.
3. El sistema presenta los certificados incluidos y el resultado común de la operación.
4. Se genera una constancia o comprobante coherente con ese resultado.
5. El ciudadano puede visualizarla o descargarla.

Flujo resumido:

```text
Pantalla de inicio
Ingreso del DNI y consulta de certificados
        ↓
Paso 1
Autenticación mediante ID Perú
        ↓
Paso 2
Selección de certificados digitales vigentes
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
| Ciudadano | Persona natural titular del DNI y de los certificados digitales relacionados con la operación | Confirmado |
| DNI | Número de identificación ingresado para iniciar la consulta | Confirmado |
| DNI electrónico o DNIe | Documento de identidad electrónico relacionado con el uso de certificados digitales. Su relación exacta con los certificados incluidos debe precisarse | Preliminar |
| Certificado digital | Elemento digital individual que vincula la identidad del titular con mecanismos criptográficos y que puede invalidarse mediante revocación | Confirmado |
| Certificado digital vigente | Término principal utilizado en la interfaz para representar un certificado asociado al ciudadano que todavía puede ser cancelado | Confirmado |
| Emisión vigente de certificado digital | Concepto que explica que el certificado fue generado en un momento determinado y constituye un elemento individual identificable | Confirmado |
| Lista de certificados vigentes | Conjunto de elementos devueltos por el servicio de consulta para un DNI | Confirmado |
| Número de orden | Dato devuelto por el servicio para identificar o diferenciar una emisión dentro de la lista | Confirmado, significado exacto pendiente |
| Fecha de creación | Fecha asociada a la generación o creación de una emisión de certificado digital | Confirmado, formato pendiente |
| UUID | Identificador único utilizado para reconocer un certificado y solicitar su revocación | Confirmado |
| Selección de certificados | Acción mediante la cual el ciudadano elige uno o varios certificados vigentes | Confirmado |
| Cancelación | Término recomendado para comunicar al ciudadano que el certificado dejará de ser válido | Confirmado |
| Revocación | Operación técnica mediante la cual un certificado digital deja de ser válido | Confirmado |
| Cancelación inmediata | Característica por la cual la revocación se ejecuta después de la confirmación, sin evaluación administrativa posterior | Confirmado |
| Autenticación | Proceso utilizado para verificar que quien realiza la operación es el titular | Confirmado |
| ID Perú | Servicio externo utilizado para autenticar la identidad del ciudadano | Confirmado |
| Consulta de certificados | Operación realizada para obtener las emisiones vigentes asociadas al DNI | Confirmado |
| Motivo de cancelación | Causa seleccionada por el ciudadano para registrar por qué realiza la operación | Confirmado |
| Otro motivo | Alternativa que permite ingresar una causa no contemplada en el catálogo | Confirmado |
| Confirmación | Manifestación expresa del ciudadano antes de ejecutar una operación inmediata e irreversible | Confirmado |
| Resultado atómico | Estado único devuelto para el conjunto completo de UUID confirmados | Confirmado |
| Resultado exitoso | Todos los certificados seleccionados fueron cancelados correctamente | Confirmado |
| Resultado fallido | Ningún certificado seleccionado fue cancelado correctamente | Confirmado |
| Resultado incierto | No se puede determinar con certeza el resultado final del conjunto seleccionado | Confirmado conceptualmente |
| Constancia o comprobante | Documento que acredita el resultado de la operación. Su nombre oficial y contenido están pendientes | Preliminar |
| Identidad digital | Concepto amplio que no debe utilizarse como objeto directo de la cancelación | Confirmado como término no recomendado para la acción |
| Credencial digital | Término genérico insuficiente para identificar con precisión el objeto de la operación | Confirmado como término no recomendado |
| Sesión activa | Término no recomendado, porque representa una conexión temporal y no una emisión de certificado digital | Confirmado como término no aplicable |

---

## 13. Reglas de negocio identificadas

| Código | Regla | Estado |
|---|---|---|
| RN-01 | El servicio está dirigido a ciudadanos que actúan como personas naturales | Confirmado |
| RN-02 | El proceso se inicia mediante el ingreso del número de DNI | Confirmado |
| RN-03 | Antes de la autenticación se debe consultar la lista de certificados digitales vigentes | Confirmado |
| RN-04 | El servicio de consulta debe devolver una lista y no solamente un valor verdadero o falso | Confirmado |
| RN-05 | Cada elemento de la lista debe contener número de orden, fecha de creación y UUID | Confirmado |
| RN-06 | Si la lista está vacía, el proceso no debe continuar hacia la autenticación | Confirmado |
| RN-07 | Si existe al menos un certificado, el ciudadano puede continuar hacia la autenticación | Confirmado |
| RN-08 | La autenticación del titular debe realizarse mediante ID Perú | Confirmado |
| RN-09 | La identidad autenticada debe corresponder al DNI ingresado | Preliminar, necesario para seguridad |
| RN-10 | La lista obtenida inicialmente debe estar disponible después de la autenticación | Confirmado |
| RN-11 | El paso de selección debe mostrarse siempre, incluso cuando exista un solo certificado | Confirmado |
| RN-12 | El ciudadano puede seleccionar uno o varios certificados | Confirmado |
| RN-13 | El ciudadano debe seleccionar al menos un certificado para continuar | Confirmado |
| RN-14 | Cada certificado se identifica técnicamente mediante su UUID | Confirmado |
| RN-15 | La selección no se basará inicialmente en tipos de certificado | Confirmado |
| RN-16 | El ciudadano debe registrar un motivo antes de confirmar | Confirmado |
| RN-17 | Debe existir una opción Otro para situaciones no incluidas en el catálogo | Confirmado |
| RN-18 | Cuando se seleccione Otro, debe habilitarse un campo para describir el motivo | Confirmado |
| RN-19 | La obligatoriedad, longitud y validaciones del texto de Otro están pendientes | Pendiente de validación |
| RN-20 | La operación debe requerir una confirmación expresa | Confirmado |
| RN-21 | El resumen debe mostrar los certificados seleccionados | Confirmado |
| RN-22 | El servicio de revocación debe recibir una lista de UUID | Confirmado |
| RN-23 | El servicio de revocación debe procesar atómicamente la lista completa y devolver un resultado común | Confirmado |
| RN-24 | La operación general puede ser exitosa, fallida o incierta; no existe resultado parcial | Confirmado |
| RN-25 | El sistema solo debe comunicar éxito cuando el servicio confirme todos los certificados seleccionados | Confirmado |
| RN-26 | Una respuesta mixta es incompatible y no debe traducirse a un resultado parcial | Confirmado |
| RN-27 | La constancia debe identificar los certificados seleccionados y reflejar su resultado común | Confirmado |
| RN-28 | La cancelación no implica cancelar el DNI, la identidad civil ni la cuenta de ID Perú | Confirmado |
| RN-29 | La operación no corresponde a una renovación | Confirmado |
| RN-30 | El término cancelación debe priorizarse en la interfaz y revocación en el contexto técnico | Confirmado |
| RN-31 | Las emisiones se presentarán al ciudadano como certificados digitales vigentes | Confirmado |
| RN-32 | Cada ingreso del DNI desde la página de inicio representa una nueva solicitud y una nueva consulta de certificados vigentes | Confirmado |
| RN-33 | El sistema no debe recuperar automáticamente pasos, selecciones, resultados ni constancias de una solicitud anterior | Confirmado |
| RN-34 | Las solicitudes anteriores deben conservarse como historial, sin convertirse en el contexto activo del nuevo ingreso | Confirmado |
| RN-35 | Una solicitud anterior no confirmada puede marcarse como abandonada cuando una nueva solicitud la sustituya | Confirmado |
| RN-36 | Una revocación confirmada en curso o con resultado incierto debe impedir temporalmente otro inicio sin revelar ni recuperar el trámite anterior | Confirmado por integridad e idempotencia |
| RN-37 | La selección queda inmutable después de la confirmación ciudadana | Confirmado |
| RN-38 | Los certificados no seleccionados quedan fuera de la operación y no cambian de estado | Confirmado |
| RN-39 | Un resultado incierto conserva la misma operación y clave de idempotencia hasta su reconciliación | Confirmado |

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

Identificador utilizado para consultar los certificados y relacionar la operación con el ciudadano.

No representa el objeto que será cancelado.

### 14.3. Certificado digital vigente

Elemento individual asociado al ciudadano que puede ser seleccionado para cancelación.

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
- Tipo de certificado.

### 14.4. Lista de certificados digitales vigentes

Resultado de la consulta inicial.

Posibles resultados conceptuales:

- Lista vacía.
- Lista con un certificado.
- Lista con varios certificados.
- Error de consulta.
- Resultado no concluyente.

La lista debe mantenerse vinculada al proceso iniciado por el ciudadano y utilizarse en el paso de selección después de la autenticación.

### 14.5. Selección de certificados

Conjunto de certificados elegidos por el ciudadano.

Información conceptual:

- UUID seleccionados.
- Fecha y hora de la selección.
- Cantidad seleccionada.
- Relación con el proceso.
- Versión o referencia de la consulta inicial, si fuera necesaria.

### 14.6. Autenticación de identidad

Proceso realizado mediante ID Perú para acreditar al titular.

Debe registrar conceptualmente:

- Inicio de autenticación.
- Resultado.
- Identidad verificada.
- Cancelación o error.
- Referencia de la operación, cuando corresponda.

### 14.7. Motivo de cancelación

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

Acción mediante la cual se solicita que los certificados seleccionados dejen de ser válidos.

Información conceptual:

- Ciudadano.
- DNI relacionado.
- Certificados seleccionados.
- Lista de UUID.
- Motivo.
- Fecha y hora.
- Confirmación del ciudadano.
- Resultado general.
- Código de operación.
- Mensaje devuelto por el servicio.

### 14.10. Resultado atómico de revocación

Respuesta común correspondiente al conjunto completo de UUID confirmados.

Información conceptual:

- Lista de UUID solicitados.
- Estado común del resultado.
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
- Cantidad de certificados seleccionados.
- Identificación de cada certificado.
- Resultado común de la operación.
- Mecanismo de verificación.

### 14.12. Servicios externos

Componentes institucionales o externos necesarios para:

- Consultar certificados.
- Autenticar al ciudadano.
- Ejecutar la revocación.
- Generar o verificar la constancia.

---

## 15. Estados y transiciones relevantes

### 15.1. Estado conceptual del proceso

| Estado | Descripción | Transición principal |
|---|---|---|
| Iniciado | El ciudadano accede al servicio | Ingreso del DNI |
| DNI ingresado | Se cuenta con un DNI con formato aceptable | Consulta de certificados |
| Consultando certificados | Se está solicitando la lista al servicio externo | Respuesta del servicio |
| Sin certificados disponibles | La lista está vacía | Finalización |
| Certificados disponibles | Existe al menos un certificado | Inicio de autenticación |
| En autenticación | El ciudadano se encuentra en ID Perú | Respuesta de ID Perú |
| Autenticación fallida o cancelada | No se acreditó al titular | Reintento o finalización |
| Autenticado | La identidad fue verificada | Selección de certificados |
| En selección | El ciudadano revisa las emisiones vigentes | Selección de uno o varios elementos |
| Certificados seleccionados | Existe al menos un UUID seleccionado | Registro del motivo |
| Motivo registrado | Se cuenta con una causa válida | Revisión y confirmación |
| Pendiente de confirmación | La operación está lista para ser autorizada | Confirmación o abandono |
| En revocación | El servicio está procesando los UUID | Resultado del servicio |
| Exitosa | Todos los certificados fueron cancelados | Generación de constancia |
| Fallida | Ningún certificado fue cancelado | Presentación del resultado |
| Incierta | No puede determinarse el resultado atómico del conjunto | Reconciliación sobre la misma operación |
| Constancia disponible | El comprobante puede consultarse o descargarse | Finalización |
| Constancia no disponible | Existe resultado de revocación, pero falló el documento | Reintento documental o soporte |

### 15.2. Estado conceptual de un certificado

| Estado | Descripción | Estado de definición |
|---|---|---|
| No consultado | El sistema todavía no lo conoce | Preliminar |
| Vigente y disponible | Puede mostrarse y seleccionarse para cancelación | Confirmado conceptualmente |
| Seleccionado | Fue elegido por el ciudadano | Confirmado |
| No seleccionado | Se mantiene fuera de la operación actual | Confirmado |
| En proceso de revocación | Su UUID fue enviado al servicio | Confirmado conceptualmente |
| Revocado | El servicio confirmó que dejó de ser válido | Confirmado |
| Revocación fallida | El servicio confirmó que no pudo cancelarse | Confirmado conceptualmente |
| Resultado incierto | No se conoce con certeza su estado final | Preliminar |
| Ya no disponible | Cambió de estado antes de procesarse | Pendiente de validación |

La clasificación exacta dependerá de los estados proporcionados por los servicios institucionales.

---

## 16. Supuestos preliminares

1. El servicio de consulta podrá obtener las emisiones vigentes asociadas a un DNI.

2. La respuesta del servicio será una lista.

3. Cada elemento incluirá un número de orden, una fecha de creación y un UUID.

4. Una lista vacía significará que no existe ningún certificado disponible para cancelar.

5. La lista obtenida podrá mantenerse vinculada al proceso hasta el paso de selección.

6. ID Perú devolverá información suficiente para comprobar la identidad del titular.

7. Será posible relacionar la autenticación de ID Perú con el DNI ingresado.

8. El ciudadano podrá diferenciar los certificados mediante la información mostrada.

9. El paso de selección se mostrará aun cuando exista un solo certificado.

10. El servicio de revocación aceptará una lista de UUID.

11. El servicio devolverá un único resultado atómico para la lista completa.

12. El resultado será exitoso, fallido o incierto; no existirá resultado parcial.

13. La revocación se ejecutará inmediatamente después de la confirmación.

14. La constancia identificará el conjunto seleccionado y reflejará su resultado común.

15. Los motivos mostrados representan un catálogo preliminar y pueden requerir ajustes.

16. La opción Otro requerirá una explicación.

17. La cancelación será irreversible respecto de cada certificado revocado.

18. Para volver a utilizar funciones que dependan de un certificado cancelado, el ciudadano podría necesitar una nueva emisión o activación. El proceso exacto está pendiente.

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
- Los UUID enviados deben pertenecer a la lista asociada al proceso.
- No debe permitirse inyectar UUID arbitrarios desde el cliente.
- Deben evitarse solicitudes duplicadas.
- El resultado debe ser trazable respecto del conjunto confirmado.
- El sistema no debe mostrar éxito sin confirmación del servicio.
- Debe analizarse el tratamiento de respuestas tardías o inciertas sobre la misma operación idempotente.
- La selección no puede alterarse después de la confirmación.
- Una respuesta mixta debe rechazarse como incompatible; no debe convertirse en un estado parcial.
- Debe evaluarse la revalidación del estado de los certificados antes de ejecutar la revocación.
- El backend debe controlar la integridad de la selección y no confiar únicamente en el frontend.

### 17.3. Privacidad

- El sistema procesará información de identificación personal.
- Debe limitarse la exposición del DNI y de los datos personales.
- La consulta se realiza antes de la autenticación, por lo que debe revisarse qué información puede mostrarse en esa etapa.
- La lista puede obtenerse en la pantalla de inicio, pero debería mostrarse al ciudadano únicamente después de autenticarlo.
- Debe evaluarse si el UUID puede mostrarse completo o debe ocultarse parcialmente.
- El periodo de conservación de listas, motivos, evidencias y resultados está pendiente.
- Debe evitarse solicitar información confidencial dentro de Otro motivo.

### 17.4. Comunicación al ciudadano

- Debe utilizarse lenguaje comprensible.
- En la interfaz se empleará **certificados digitales vigentes**.
- Cuando sea necesario explicar la coexistencia de varios elementos, se utilizará **emisiones vigentes de certificados digitales**.
- No debe indicarse que se revoca la identidad del ciudadano.
- No debe afirmarse que se cancela el DNI.
- No deben comunicarse efectos que el servicio real no garantice.
- Debe diferenciarse entre cancelar el trámite y cancelar certificados.
- Debe indicarse claramente que el ciudadano puede seleccionar uno o varios.
- La consecuencia inmediata debe presentarse antes de la confirmación.
- El resultado debe comunicarse como exitoso, fallido o incierto para todo el conjunto; no debe sugerirse un éxito parcial.

### 17.5. Accesibilidad y usabilidad

- El flujo debe ser comprensible para ciudadanos con distintos niveles de conocimiento digital.
- La lista debe permitir diferenciar claramente cada certificado.
- La selección debe funcionar con teclado y tecnologías de asistencia.
- Los mensajes no deben depender únicamente del color.
- Debe existir una indicación clara del número de certificados seleccionados.
- Cuando exista un solo certificado, el paso debe seguir siendo comprensible y no parecer innecesario.
- Los criterios institucionales de accesibilidad están pendientes de validación.

### 17.6. Dependencias externas

El funcionamiento depende de:

- Servicio de consulta de certificados.
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
- Forma válida de acreditar la selección de certificados.
- Tratamiento legal de resultados inciertos y su reconciliación.

---

## 18. Información confirmada

- El sistema será desarrollado para RENIEC.
- El usuario principal será un ciudadano que actúa como persona natural.
- El objeto de la operación son los certificados digitales vigentes.
- Los certificados se encuentran asociados al DNI del ciudadano.
- Un ciudadano puede tener una o varias emisiones vigentes.
- Las diferentes emisiones pueden haber sido generadas en distintos momentos.
- En la interfaz se utilizará el término certificados digitales vigentes.
- El término emisiones vigentes se utilizará para dar precisión conceptual.
- No se utilizará el término sesiones activas para representar estos certificados.
- El ciudadano ingresará su número de DNI al inicio.
- El primer servicio web devolverá una lista de certificados, no solamente verdadero o falso.
- Cada elemento tendrá número de orden, fecha de creación y UUID.
- Si la lista está vacía, el proceso no continuará.
- Si existe al menos un certificado, el ciudadano podrá continuar.
- La consulta se realizará antes de la autenticación con ID Perú.
- ID Perú se utilizará para autenticar la identidad.
- ID Perú no es el objeto de la cancelación.
- Después de autenticarse, el ciudadano verá el paso de certificados vigentes.
- El paso de selección siempre se mostrará.
- Esto también se aplica cuando exista un solo certificado.
- El ciudadano podrá seleccionar uno o varios certificados.
- Debe seleccionar al menos uno.
- El ciudadano deberá seleccionar un motivo.
- Se incluirá la opción Otro motivo.
- Al seleccionar Otro, podrá indicar una causa no contemplada.
- La confirmación mostrará los certificados seleccionados.
- El servicio de revocación recibirá una lista de UUID.
- La selección quedará inmutable después de la confirmación.
- El servicio procesará la lista completa de forma atómica y devolverá un resultado común.
- La operación puede tener un resultado exitoso, fallido o incierto; no existe resultado parcial.
- Los certificados no seleccionados permanecerán fuera de la operación y no cambiarán de estado.
- La constancia deberá reflejar el resultado real.
- La operación será una cancelación inmediata.
- Técnicamente, corresponde a una revocación.
- El sistema no cancela la identidad civil, el DNI ni la cuenta de ID Perú.
- La operación no corresponde a una renovación.

---

## 19. Aspectos pendientes de validación

### Prioridad alta

1. **Alcance exacto de los certificados**  
   Determinar qué certificados están incluidos y cómo se relacionan con el DNIe.

2. **Criterio de vigencia y elegibilidad**
   Precisar qué estados hacen que un certificado aparezca en la lista.

3. **Contrato del servicio de consulta**
   Definir solicitud, estructura de respuesta, errores, códigos y límites.

4. **Semántica del número de orden**
   Confirmar si representa secuencia de emisión, posición de la lista u otro dato institucional.

5. **Fecha de creación**
   Confirmar su significado, zona horaria y formato.

6. **Uso y exposición del UUID**
   Definir si se muestra completo, parcial o mediante un identificador alternativo.

7. **Contrato del servicio de revocación**
   Confirmar que recibe la lista completa de UUID y una clave de idempotencia, y que garantiza un único resultado atómico.

8. **Resultado incierto y reconciliación**
   Precisar códigos, mensajes y consultas permitidas sobre la misma operación idempotente.

9. **Correspondencia entre DNI e identidad autenticada**
   Confirmar cómo se validará técnicamente.

10. **Cambios de estado durante el flujo**
    Definir qué ocurre si un certificado deja de estar disponible después de la consulta inicial.

11. **Efectos exactos de la revocación**
    Determinar qué funciones dejarán de estar disponibles.

12. **Idempotencia y duplicidad**
    Definir el comportamiento ante reintentos o envíos repetidos.

### Prioridad media

13. **Información visible en el paso de selección**
    Validar textos, orden, ocultamiento y datos complementarios.

14. **Selección de todos**
    Confirmar si existirá una acción para marcar o desmarcar todos los certificados.

15. **Catálogo definitivo de motivos**
    Validar nombres, descripciones y códigos institucionales.

16. **Reglas de Otro motivo**
    Determinar obligatoriedad, longitud, caracteres permitidos y tratamiento de información sensible.

17. **Contenido de la confirmación**
    Aprobar textos legales, advertencias y consecuencias.

18. **Constancia o comprobante**
    Definir nombre oficial, campos, formato, mecanismo de verificación y valor institucional.

19. **Constancia para el conjunto confirmado**
    Determinar cómo documentar la lista seleccionada y su único resultado exitoso, fallido o incierto.

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

29. Mensajes para certificados vencidos o previamente revocados.

30. Comportamiento cuando falla la generación de la constancia.

31. Ordenamiento de la lista de certificados.

32. Paginación o cantidad máxima de certificados, si fuera aplicable.

33. Revalidación de la lista antes de confirmar.

---

## 20. Riesgos de interpretación

### 20.1. Confundir certificados digitales con identidad digital

El sistema no elimina ni revoca la identidad de la persona. La identidad digital no debe presentarse como objeto de la operación.

### 20.2. Confundir cancelación con revocación

Ambos términos representan perspectivas distintas:

- Cancelación: comunicación ciudadana.
- Revocación: operación técnica.

### 20.3. Confundir DNI con certificados digitales

El DNI se utiliza para identificar al ciudadano e iniciar la consulta. No es el elemento cancelado.

### 20.4. Confundir DNI con DNI electrónico

El sistema solicita el número de DNI, pero los certificados pueden estar relacionados con el contexto del DNIe. La relación exacta debe documentarse.

### 20.5. Confundir varias emisiones con varios tipos de certificado

La existencia de varios certificados vigentes no implica necesariamente que existan distintas clases o tipos. Pueden ser emisiones generadas en momentos diferentes.

### 20.6. Utilizar el término sesiones activas

Una sesión es una conexión temporal. No representa adecuadamente una emisión de certificado digital y podría llevar a pensar que la operación equivale a cerrar una sesión.

### 20.7. Confundir emisión con duplicado

Cada emisión corresponde a un certificado individual con UUID propio. No debe comunicarse necesariamente como una copia del mismo archivo o certificado.

### 20.8. Suponer selección automática cuando existe un solo certificado

Aunque exista un único elemento, el paso de selección siempre debe mostrarse y requerir una acción expresa del ciudadano.

### 20.9. Interpretar que se cancelan todos los certificados

El ciudadano puede seleccionar uno o varios. Los no seleccionados quedan fuera de la operación actual.

### 20.10. Aceptar una respuesta mixta del proveedor

Una respuesta diferente por UUID contradice la regla de todos o ninguno. Debe rechazarse como contrato incompatible y nunca comunicarse como éxito parcial.

### 20.11. Interpretar ID Perú como la credencial cancelada

ID Perú se utiliza para autenticar al titular. La operación no cancela ID Perú.

### 20.12. Interpretar la cancelación como una solicitud diferida

La operación está planteada como inmediata después de la confirmación.

### 20.13. Afirmar efectos no confirmados

Revocar certificados no necesariamente implica:

- Eliminar una aplicación.
- Retirar automáticamente un DNI digital.
- Cerrar todas las sesiones.
- Bloquear todos los servicios del Estado.
- Desvincular un dispositivo.
- Cancelar la identidad del ciudadano.

### 20.14. Confundir ausencia de certificados con certificados ya revocados

El servicio podría distinguir varios estados. No debe asumirse que todos equivalen a “no tiene certificados”.

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

El proyecto consiste en un sistema web institucional para RENIEC, dirigido a personas naturales, que permite cancelar de manera inmediata uno o varios certificados digitales vigentes asociados a un DNI.

Un ciudadano puede tener una o más emisiones vigentes de certificados digitales generadas en momentos diferentes. En la interfaz, estas emisiones se presentan como certificados digitales vigentes. Cada certificado se identifica mediante un número de orden, una fecha de creación y un UUID único.

El flujo comienza en una pantalla de inicio donde el ciudadano ingresa su DNI. Un primer servicio web devuelve una lista de certificados digitales vigentes. Si la lista está vacía, el proceso finaliza. Si contiene al menos un elemento, el ciudadano continúa con la autenticación mediante ID Perú.

Después de autenticarse, se muestra el paso de selección de certificados. Este paso siempre se presenta, incluso cuando solo existe un certificado. El ciudadano debe seleccionar uno o varios certificados para continuar.

Luego selecciona el motivo de cancelación, revisa un resumen y confirma expresamente la operación. La selección queda inmutable. El sistema envía al servicio de revocación la lista completa de UUID seleccionados bajo una única clave de idempotencia. El servicio debe procesarla de forma atómica: el resultado es exitoso si revoca todos, fallido si no revoca ninguno o incierto mientras no pueda confirmarse uno de esos resultados.

Finalmente, el sistema muestra el conjunto procesado y su resultado común, y genera una constancia coherente. Los certificados no seleccionados permanecen fuera de la operación.

Cada ingreso posterior del DNI desde la página de inicio inicia una solicitud nueva y una consulta actualizada. El progreso, la selección y la constancia de solicitudes anteriores no se recuperan automáticamente. El historial se conserva para evidencia y trazabilidad; una revocación todavía en curso o incierta solo puede bloquear temporalmente un nuevo inicio para evitar operaciones duplicadas.

En la comunicación ciudadana se utilizará cancelación de certificados digitales; técnicamente la operación corresponde a una revocación. El sistema no cancela la identidad civil, el DNI, el DNIe ni la cuenta de ID Perú. Tampoco debe utilizarse el término sesiones activas para representar los certificados.

Permanecen pendientes los contratos definitivos de los servicios, los estados exactos, la presentación del UUID, los efectos de la revocación, el contenido de la constancia, las reglas de auditoría, privacidad y normativa aplicable.

---

## 23. Contexto compacto para prompts

> Proyecto web de RENIEC para que personas naturales cancelen inmediatamente uno o varios certificados digitales vigentes asociados a su DNI. Un ciudadano puede tener varias emisiones vigentes generadas en momentos distintos; en la interfaz se mostrarán como certificados digitales vigentes. El ciudadano ingresa su DNI en la pantalla de inicio y un servicio devuelve una lista de certificados con número de orden, fecha de creación y UUID. Si la lista está vacía, el proceso no continúa. Si existe al menos un certificado, el ciudadano se autentica mediante ID Perú. Después accede siempre al paso de selección, incluso cuando exista un solo certificado, y debe elegir uno o varios. Luego registra el motivo, revisa las consecuencias y confirma; desde ese momento la selección es inmutable. El servicio de revocación recibe la lista completa de UUID con una clave de idempotencia y aplica todos o ninguno. La operación puede ser exitosa, fallida o incierta, nunca parcial, y la constancia identifica el conjunto seleccionado y su resultado común. Los certificados no seleccionados no se alteran. Cada ingreso posterior desde inicio crea una solicitud y consulta nuevas; no recupera el progreso, la selección ni la constancia anterior, aunque el historial se conserva. En la interfaz debe utilizarse “cancelación de certificados digitales”; técnicamente la operación es una revocación. No se cancela la identidad civil, el DNI, el DNIe ni ID Perú. No debe utilizarse el término “sesiones activas” para representar las emisiones vigentes.
