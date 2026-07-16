# Contexto del proyecto

## 1. Resumen ejecutivo

El proyecto consiste en el diseño de un sistema web institucional para el **Registro Nacional de Identificación y Estado Civil (RENIEC) del Perú**, dirigido a ciudadanos que actúan como personas naturales.

Su propósito es proporcionar un canal digital de autoservicio mediante el cual el titular pueda **cancelar de forma inmediata los certificados digitales asociados a su DNI**, cuando exista una situación que pueda comprometer su seguridad, como pérdida, robo, cambio de equipo o sospecha de uso no autorizado.

En la comunicación dirigida al ciudadano se utilizará preferentemente el término **cancelación de certificados digitales**. Desde la perspectiva técnica y del dominio de certificación digital, la operación ejecutada corresponde a una **revocación**.

El sistema no cancela la identidad civil, el número de DNI, el documento físico, el DNI electrónico ni la cuenta de ID Perú. Su objeto funcional son los **certificados digitales**, tratados para este proceso como un conjunto único, sin permitir que el ciudadano visualice o seleccione certificados individuales.

El proceso preliminar comprende:

1. Ingreso del número de DNI.
2. Consulta de existencia de certificados digitales.
3. Autenticación del titular mediante ID Perú.
4. Selección del motivo de cancelación.
5. Confirmación informada de la operación.
6. Ejecución inmediata de la revocación.
7. Presentación y descarga de una constancia o comprobante.

El sistema dependerá de servicios externos para consultar la existencia de certificados digitales, autenticar al ciudadano y ejecutar la revocación. Las características técnicas, contratos y respuestas exactas de estos servicios todavía se encuentran pendientes de validación.

---

## 2. Antecedentes y contexto

RENIEC es la institución pública responsable de la identificación de las personas y del registro de los hechos relativos al estado civil en el Perú. Dentro de su ámbito de identidad y certificación digital, los ciudadanos pueden contar con certificados digitales vinculados a su identificación y al uso del DNI electrónico.

Los certificados digitales pueden utilizarse para acreditar identidad en entornos digitales, realizar operaciones de autenticación y ejecutar funciones relacionadas con la firma digital. Mientras permanecen vigentes, pueden ser aceptados por sistemas y servicios que confían en ellos.

Determinadas situaciones pueden hacer necesario que el titular impida su utilización, especialmente cuando:

- Pierde el dispositivo o elemento relacionado con su uso.
- Sufre el robo de dicho dispositivo.
- Cambia de equipo o número asociado.
- Sospecha que sus mecanismos digitales fueron comprometidos.
- Presenta otra circunstancia que justifica la cancelación.

El proyecto busca establecer un canal remoto, comprensible y seguro para que una persona natural pueda ejecutar esta operación sin administrar detalles técnicos de cada certificado.

Los prototipos funcionales disponibles muestran un flujo basado en identificación mediante DNI, autenticación con ID Perú, registro del motivo, confirmación de la operación y emisión de un comprobante.

La base normativa específica, las políticas institucionales aplicables, las responsabilidades operativas y el procedimiento vigente de cancelación todavía deben ser documentados y validados formalmente.

---

## 3. Definición del problema

### 3.1. Situación identificada

Un ciudadano puede tener certificados digitales asociados a su DNI que continúen vigentes aun cuando se produzca una situación de riesgo, como pérdida, robo, cambio de equipo o sospecha de acceso no autorizado.

En estos casos, el titular necesita contar con un mecanismo que le permita invalidarlos oportunamente para impedir que sigan siendo aceptados como certificados válidos.

No se dispone todavía de información confirmada sobre la forma en que este procedimiento se realiza actualmente, si existe un canal digital previo, si requiere atención presencial o si depende de intervención administrativa.

### 3.2. Dificultades preliminares

A partir de la información disponible, se identifican las siguientes dificultades:

- El ciudadano podría no contar con un canal digital directo para cancelar sus certificados.
- La demora en la revocación podría prolongar el periodo de exposición ante un posible uso no autorizado.
- Los conceptos de identidad digital, DNI electrónico, credencial digital y certificado digital pueden resultar confusos para el ciudadano.
- El titular no necesariamente conoce cuántos certificados existen, cuáles son o cómo se administran técnicamente.
- La operación requiere verificar previamente que el DNI tenga certificados susceptibles de revocación.
- La cancelación debe ser autorizada exclusivamente por el titular, por lo que se necesita un mecanismo confiable de autenticación.
- El ciudadano necesita recibir evidencia de que la operación fue procesada.

### 3.3. Causas preliminares

Las causas del problema todavía requieren análisis formal. Como hipótesis iniciales se consideran:

- Ausencia o insuficiencia de un canal de autoservicio especializado.
- Dependencia de procesos operativos que podrían no ser inmediatos.
- Complejidad técnica del dominio de certificación digital.
- Falta de claridad en la comunicación ciudadana sobre qué elemento debe cancelarse.
- Necesidad de integrar diferentes servicios institucionales para verificar, autenticar y revocar.

### 3.4. Consecuencias posibles

Mientras los certificados permanezcan vigentes después de una situación de riesgo, podrían producirse:

- Intentos de autenticación no autorizada.
- Uso indebido de mecanismos de identificación digital.
- Riesgo de operaciones realizadas sin autorización del titular.
- Incertidumbre del ciudadano sobre el estado de sus certificados.
- Dificultad para acreditar que la cancelación fue solicitada y ejecutada.
- Pérdida de confianza en los servicios digitales institucionales.

Estas consecuencias son interpretaciones derivadas del propósito del proyecto y deberán alinearse con el alcance real de los certificados involucrados.

### 3.5. Necesidad que justifica el sistema

Se requiere un servicio web que permita al ciudadano:

- Verificar si cuenta con certificados digitales que puedan cancelarse.
- Demostrar que es el titular mediante ID Perú.
- Registrar el motivo de la operación.
- Comprender las consecuencias antes de confirmar.
- Ejecutar la revocación de forma inmediata.
- Obtener una constancia verificable del resultado.

---

## 4. Objetivo general del proyecto

Implementar un servicio web institucional de RENIEC que permita a una persona natural cancelar de manera segura e inmediata los certificados digitales asociados a su DNI, previa verificación de su existencia, autenticación del titular mediante ID Perú, registro del motivo y confirmación expresa de la operación.

---

## 5. Objetivos específicos preliminares

- Permitir que el ciudadano inicie el proceso utilizando su número de DNI.
- Verificar mediante un servicio institucional si el DNI tiene certificados digitales susceptibles de revocación.
- Evitar que continúe el proceso cuando no existan certificados digitales que puedan cancelarse.
- Autenticar al titular mediante el servicio externo ID Perú.
- Registrar el motivo por el cual se solicita la cancelación.
- Incorporar una opción abierta para motivos no contemplados en el catálogo.
- Informar al ciudadano sobre el carácter inmediato y las consecuencias de la operación.
- Obtener una confirmación explícita antes de ejecutar la revocación.
- Solicitar la revocación de los certificados digitales como un conjunto funcional.
- Comunicar el resultado de la operación de forma clara.
- Generar una constancia o comprobante de la cancelación realizada.

---

## 6. Descripción general de la solución

La solución propuesta es un **portal web de cancelación de certificados digitales para personas naturales**.

El ciudadano ingresará su número de DNI. El sistema consultará un servicio externo para determinar si existen certificados digitales asociados que puedan ser revocados. Cuando la respuesta sea favorable, se solicitará al ciudadano que autentique su identidad mediante ID Perú.

Una vez autenticado, el titular seleccionará el motivo de la cancelación. El sistema ofrecerá un conjunto de motivos predefinidos y una alternativa denominada **Otro motivo**, que permitirá registrar una explicación adicional.

Antes de ejecutar la operación, el sistema presentará un resumen y una advertencia sobre sus consecuencias. La revocación solo se solicitará después de la confirmación expresa del ciudadano.

El servicio de revocación procesará conjuntamente los certificados digitales relacionados con el DNI. El ciudadano no visualizará una lista ni podrá elegir certificados específicos.

Cuando la operación concluya correctamente, el sistema mostrará el resultado y pondrá a disposición una constancia o comprobante.

---

## 7. Alcance funcional preliminar

### 7.1. Procesos incluidos

El alcance preliminar comprende:

- Ingreso y validación básica del número de DNI.
- Consulta de existencia de certificados digitales.
- Determinación de si el ciudadano puede continuar.
- Autenticación mediante ID Perú.
- Registro del motivo de cancelación.
- Registro de un motivo personalizado mediante la opción Otro.
- Presentación de advertencias y consecuencias.
- Confirmación expresa de la cancelación.
- Solicitud de revocación mediante un servicio externo.
- Presentación del resultado.
- Generación y descarga de una constancia o comprobante.
- Manejo general de respuestas negativas, errores y falta de disponibilidad.

### 7.2. Acciones generales disponibles para el ciudadano

- Ingresar su DNI.
- Continuar o abandonar el proceso.
- Autenticarse mediante ID Perú.
- Seleccionar un motivo.
- Escribir un motivo alternativo.
- Revisar la información antes de confirmar.
- Confirmar la cancelación.
- Consultar el resultado.
- Descargar una constancia.
- Finalizar el proceso.

### 7.3. Elementos fuera del alcance preliminar

No forman parte del alcance confirmado:

- Emisión de nuevos certificados digitales.
- Renovación de certificados digitales.
- Selección individual de certificados.
- Visualización de detalles técnicos de cada certificado.
- Reactivación de certificados revocados.
- Cancelación del número de DNI.
- Cancelación o bloqueo del documento físico.
- Eliminación de la identidad civil del ciudadano.
- Cancelación de la cuenta de ID Perú.
- Administración general de identidades digitales.
- Gestión de usuarios institucionales.
- Módulos administrativos o de atención interna.
- Recuperación de credenciales.
- Gestión de dispositivos.
- Cierre de sesiones en otros sistemas.
- Bloqueo general de todos los servicios digitales del ciudadano.

### 7.4. Aspectos del alcance pendientes de confirmación

- Tipo exacto de certificados incluidos.
- Criterio utilizado para determinar que un certificado puede revocarse.
- Tratamiento de certificados vencidos o previamente revocados.
- Datos mostrados antes de la confirmación.
- Alcance exacto de los efectos posteriores.
- Contenido y validez de la constancia.
- Existencia de canales de soporte o derivación.
- Reglas para reintentos y operaciones con resultado incierto.

---

## 8. Usuarios y actores involucrados

| Actor | Rol dentro del proceso | Necesidad principal | Acciones o responsabilidades | Situaciones de participación |
|---|---|---|---|---|
| Ciudadano o persona natural | Usuario principal y titular de los certificados digitales | Cancelar oportunamente sus certificados ante una situación de riesgo | Ingresar DNI, autenticarse, seleccionar motivo, confirmar y descargar la constancia | Robo, pérdida, cambio de equipo o número, sospecha de uso no autorizado u otro motivo |
| RENIEC | Institución responsable del servicio | Proporcionar un canal confiable para la cancelación de certificados digitales | Administrar el servicio, definir reglas, comunicar resultados y garantizar la trazabilidad institucional | Durante todo el proceso |
| ID Perú | Servicio externo de autenticación | Confirmar que la persona que realiza el proceso es quien afirma ser | Autenticar al ciudadano y devolver el resultado correspondiente | Después de verificar la existencia de certificados |
| Servicio de consulta de certificados | Sistema externo colaborador | Determinar si el DNI tiene certificados digitales susceptibles de revocación | Recibir la consulta y devolver el resultado de la verificación | Después del ingreso del DNI |
| Servicio de revocación | Sistema externo colaborador | Ejecutar la revocación de los certificados digitales | Procesar la operación y devolver su resultado | Después de la confirmación del ciudadano |
| Servicio o mecanismo de constancias | Actor conceptual pendiente de validación | Generar evidencia de la operación | Producir o proporcionar el comprobante correspondiente | Después de una revocación exitosa |
| Personal de soporte o atención | Actor potencial pendiente de validación | Atender incidencias o casos que no puedan resolverse en línea | Orientar, revisar incidencias o derivar casos | Errores, indisponibilidad o situaciones excepcionales |

---

## 9. Escenarios principales de uso

### 9.1. Robo

El ciudadano sufre el robo del dispositivo o elemento relacionado con el uso de sus certificados digitales y necesita impedir que continúen siendo utilizados.

### 9.2. Pérdida

El ciudadano pierde el dispositivo o medio asociado y requiere cancelar sus certificados como medida de protección.

### 9.3. Cambio de equipo o número

El ciudadano cambia de dispositivo, equipo o número relacionado con el servicio y considera necesario cancelar los certificados anteriores.

El efecto real del cambio de número sobre los certificados debe confirmarse.

### 9.4. Sospecha de uso no autorizado

El ciudadano identifica señales de posible compromiso, acceso no reconocido o utilización indebida y decide cancelar sus certificados preventivamente.

### 9.5. Otro motivo

La situación del ciudadano no corresponde a los motivos predefinidos. Selecciona la opción Otro e ingresa una descripción breve.

### 9.6. DNI sin certificados disponibles

El ciudadano ingresa su DNI, pero el servicio determina que no existen certificados susceptibles de revocación. El proceso debe finalizar sin solicitar autenticación ni ejecutar una cancelación.

### 9.7. Autenticación no completada

El ciudadano no logra autenticarse, cancela el proceso de ID Perú o recibe un resultado negativo. La revocación no debe ejecutarse.

### 9.8. Revocación exitosa

El ciudadano completa la autenticación, registra el motivo y confirma la operación. El servicio procesa la revocación y se genera la constancia.

### 9.9. Resultado fallido o incierto

El servicio no puede confirmar la revocación, se encuentra indisponible o devuelve una respuesta no concluyente. El sistema no debe comunicar éxito mientras el resultado no esté confirmado.

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
- Cómo se entrega la constancia.
- Qué problemas concretos presenta el proceso vigente.
- Si el futuro sistema reemplazará, complementará o ampliará un canal existente.

No debe asumirse que el proceso actual es manual, presencial o ineficiente hasta contar con evidencia institucional.

---

## 11. Proceso esperado

El proceso funcional esperado es el siguiente:

1. **Ingreso del DNI**  
   El ciudadano ingresa su número de DNI.

2. **Consulta de certificados digitales**  
   El sistema consulta un servicio externo para verificar si existen certificados que puedan revocarse.

3. **Evaluación de continuidad**  
   Si no existen certificados disponibles, se informa al ciudadano y se finaliza el proceso. Si existen, se continúa.

4. **Autenticación mediante ID Perú**  
   El ciudadano acredita su identidad utilizando ID Perú.

5. **Selección del motivo**  
   El titular selecciona una causa predefinida o la opción Otro.

6. **Registro de un motivo alternativo**  
   Cuando se selecciona Otro, el ciudadano escribe una descripción breve.

7. **Revisión y confirmación**  
   El sistema presenta un resumen y comunica que la operación será inmediata y no podrá deshacerse sobre los certificados revocados.

8. **Ejecución de la revocación**  
   Después de la confirmación, el sistema solicita al servicio correspondiente que revoque los certificados digitales asociados al DNI.

9. **Comunicación del resultado**  
   El sistema informa si la operación fue exitosa o si no pudo completarse.

10. **Generación de la constancia**  
    Cuando la revocación es confirmada, el ciudadano puede visualizar o descargar el comprobante correspondiente.

Flujo resumido:

```text
Ingreso del DNI
        ↓
Consulta de certificados digitales
        ↓
Autenticación mediante ID Perú
        ↓
Selección del motivo
        ↓
Confirmación de la cancelación
        ↓
Revocación de los certificados
        ↓
Resultado y constancia
```

---

## 12. Conceptos y terminología del dominio

| Término | Definición dentro del proyecto | Estado |
|---|---|---|
| RENIEC | Institución pública peruana responsable del servicio y del contexto de identificación y certificación digital del proyecto | Confirmado |
| Ciudadano | Persona natural titular del DNI y de los certificados digitales relacionados con la operación | Confirmado |
| DNI | Número de identificación ingresado por el ciudadano para iniciar la consulta | Confirmado |
| DNI electrónico o DNIe | Documento de identidad electrónico relacionado con el uso de certificados digitales. La delimitación exacta de su relación con los certificados incluidos en el sistema debe precisarse | Preliminar |
| Certificado digital | Elemento digital que vincula la identidad del titular con mecanismos criptográficos y que puede ser invalidado mediante revocación | Confirmado |
| Certificados digitales | Conjunto funcional asociado al DNI que será tratado como una sola unidad durante la cancelación | Confirmado |
| Cancelación | Término recomendado para comunicar al ciudadano que sus certificados dejarán de ser válidos | Confirmado |
| Revocación | Operación técnica mediante la cual los certificados digitales dejan de ser válidos | Confirmado |
| Cancelación inmediata | Característica del proceso por la cual la revocación se ejecutará al confirmar la operación, sin tratarse únicamente de una solicitud para evaluación posterior | Confirmado |
| Autenticación | Proceso utilizado para verificar que quien realiza la operación es el titular | Confirmado |
| ID Perú | Servicio externo existente que se utilizará para autenticar la identidad del ciudadano | Confirmado |
| Consulta de certificados | Verificación realizada mediante un servicio externo para determinar si el DNI tiene certificados susceptibles de revocación | Confirmado |
| Motivo de cancelación | Causa seleccionada por el ciudadano para registrar por qué realiza la operación | Confirmado |
| Otro motivo | Alternativa que permite ingresar una causa no contemplada en el catálogo | Confirmado |
| Confirmación | Manifestación expresa del ciudadano antes de ejecutar una operación inmediata e irreversible sobre los certificados | Confirmado |
| Constancia o comprobante | Documento que acredita el resultado de la operación. Su nombre oficial, contenido y mecanismo de validación están pendientes | Preliminar |
| Identidad digital | Concepto amplio que no debe utilizarse como objeto directo de la cancelación, porque el sistema no elimina la identidad del ciudadano | Confirmado como término no recomendado para la acción |
| Credencial digital | Término genérico utilizado inicialmente, pero insuficiente para identificar con precisión el objeto de la operación | Confirmado como término no recomendado |
| Elegibilidad para cancelar | Condición por la cual el servicio determina que existen certificados susceptibles de revocación | Pendiente de validación |
| Estado de revocación | Resultado técnico que indica que los certificados ya no deben considerarse válidos | Preliminar |

---

## 13. Reglas de negocio identificadas

| Código | Regla | Estado |
|---|---|---|
| RN-01 | El servicio está dirigido a ciudadanos que actúan como personas naturales | Confirmado |
| RN-02 | El proceso se inicia mediante el ingreso del número de DNI | Confirmado |
| RN-03 | Antes de la autenticación con ID Perú se debe consultar si el DNI tiene certificados digitales susceptibles de revocación | Confirmado |
| RN-04 | Si no existen certificados disponibles para cancelar, el proceso no debe continuar hacia la autenticación ni la revocación | Interpretación derivada |
| RN-05 | La autenticación del titular debe realizarse mediante ID Perú | Confirmado |
| RN-06 | La identidad autenticada debería corresponder al DNI ingresado al inicio | Preliminar |
| RN-07 | Los certificados digitales se tratarán como un conjunto funcional único | Confirmado |
| RN-08 | El ciudadano no podrá visualizar ni seleccionar certificados individuales | Confirmado |
| RN-09 | El ciudadano debe registrar un motivo antes de confirmar la cancelación | Confirmado |
| RN-10 | Debe existir una opción Otro para situaciones no incluidas en el catálogo | Confirmado |
| RN-11 | Cuando se seleccione Otro, debe habilitarse un campo para describir el motivo | Confirmado |
| RN-12 | La obligatoriedad, longitud y validaciones del texto ingresado en Otro están pendientes de definición | Pendiente de validación |
| RN-13 | La operación debe requerir una confirmación expresa antes de ejecutarse | Interpretación derivada de los prototipos |
| RN-14 | La revocación se ejecutará de forma inmediata después de la confirmación | Confirmado |
| RN-15 | El sistema no debe comunicar éxito hasta recibir confirmación de la revocación | Preliminar |
| RN-16 | Una operación exitosa debe producir una constancia o comprobante | Interpretación derivada de los prototipos |
| RN-17 | La cancelación de certificados no implica cancelar el DNI, la identidad civil ni la cuenta de ID Perú | Confirmado |
| RN-18 | La operación no corresponde a una renovación de certificados | Confirmado |
| RN-19 | El término cancelación debe priorizarse en la interfaz ciudadana y revocación en el contexto técnico | Confirmado |

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

Identificador utilizado para consultar la existencia de certificados y relacionar la operación con el ciudadano.

No representa el objeto que será cancelado.

### 14.3. Certificados digitales

Conjunto de certificados asociados al DNI que serán tratados como una unidad funcional.

El ciudadano no administrará:

- Tipo de certificado.
- Número de serie.
- Fecha de emisión individual.
- Emisor individual.
- Selección parcial.

La disponibilidad real de estos datos en los servicios está pendiente de validación.

### 14.4. Verificación de certificados

Resultado de la consulta inicial que permite determinar si el proceso puede continuar.

Posibles resultados conceptuales:

- Existen certificados susceptibles de revocación.
- No existen certificados susceptibles de revocación.
- No fue posible realizar la consulta.
- Resultado no concluyente.

### 14.5. Autenticación de identidad

Proceso realizado mediante ID Perú para acreditar al titular.

Debe registrar, conceptualmente:

- Inicio de autenticación.
- Resultado.
- Identidad verificada.
- Cancelación o error.
- Referencia de la operación, cuando corresponda.

### 14.6. Motivo de cancelación

Causa declarada por el ciudadano.

Catálogo preliminar:

- Robo.
- Pérdida.
- Cambio de equipo o número.
- Sospecha de uso no autorizado.
- Otro motivo.

### 14.7. Descripción de otro motivo

Texto ingresado cuando las alternativas predefinidas no representan la situación del ciudadano.

### 14.8. Operación de revocación

Acción mediante la cual se solicita que los certificados digitales dejen de ser válidos.

Información conceptual:

- Titular.
- DNI relacionado.
- Motivo.
- Fecha y hora.
- Confirmación del ciudadano.
- Resultado de la revocación.
- Código de operación.
- Mensaje devuelto por el servicio.

### 14.9. Constancia o comprobante

Documento que acredita el resultado de una operación exitosa.

Su contenido exacto está pendiente de validación, pero preliminarmente podría incluir:

- Código de operación.
- DNI parcialmente oculto.
- Identificación parcial del titular.
- Fecha y hora.
- Motivo.
- Resultado.
- Estado final.
- Mecanismo de verificación.

### 14.10. Servicios externos

Componentes institucionales o externos necesarios para:

- Consultar certificados.
- Autenticar al ciudadano.
- Ejecutar la revocación.
- Generar o verificar la constancia.

---

## 15. Estados y transiciones relevantes

### 15.1. Estado conceptual de la operación

| Estado | Descripción | Transición principal |
|---|---|---|
| Iniciada | El ciudadano accede al servicio | Ingreso del DNI |
| DNI ingresado | Se cuenta con un número de DNI con formato aceptable | Consulta de certificados |
| En verificación | Se está consultando la existencia de certificados | Respuesta del servicio |
| No elegible | No se encontraron certificados susceptibles de revocación | Finalización del proceso |
| Elegible | Existen certificados que permiten continuar | Inicio de autenticación |
| En autenticación | El ciudadano se encuentra en el proceso de ID Perú | Respuesta de ID Perú |
| Autenticación fallida o cancelada | No se acreditó al titular | Reintento o finalización |
| Autenticada | La identidad fue verificada | Selección del motivo |
| Motivo registrado | Se cuenta con una causa válida | Revisión y confirmación |
| Pendiente de confirmación | La operación está lista para ser autorizada | Confirmación o abandono |
| En revocación | El servicio está procesando la cancelación | Resultado del servicio |
| Exitosa | La revocación fue confirmada | Generación de constancia |
| Fallida | El servicio confirmó que la operación no pudo ejecutarse | Reintento o canal de soporte |
| Incierta | No se conoce con certeza el resultado | Consulta de estado o tratamiento excepcional |
| Constancia disponible | El comprobante puede consultarse o descargarse | Finalización |

Los nombres definitivos de estos estados están pendientes de validación funcional.

### 15.2. Estado conceptual de los certificados

| Estado | Descripción | Estado de definición |
|---|---|---|
| No consultado | El sistema todavía no verificó la existencia de certificados | Preliminar |
| Disponible para revocación | Existen certificados que permiten continuar | Preliminar |
| No disponible para revocación | No existen certificados aplicables | Preliminar |
| En proceso de revocación | Se solicitó la operación, pero todavía no existe resultado confirmado | Preliminar |
| Revocado | Los certificados dejaron de ser válidos | Confirmado conceptualmente |
| Resultado desconocido | No se pudo determinar si la operación se completó | Preliminar |

La clasificación exacta dependerá de los estados proporcionados por los servicios institucionales.

---

## 16. Supuestos preliminares

1. El servicio de consulta podrá determinar si un DNI tiene certificados digitales susceptibles de revocación.

2. La consulta inicial devolverá información suficiente para decidir si el ciudadano puede continuar, sin necesidad de mostrar detalles de cada certificado.

3. ID Perú devolverá información suficiente para comprobar la identidad del titular.

4. Será posible relacionar la autenticación de ID Perú con el DNI ingresado al inicio.

5. El servicio de revocación operará sobre todos los certificados digitales aplicables como un solo conjunto funcional.

6. La revocación se ejecutará inmediatamente después de la confirmación.

7. El sistema recibirá una respuesta que permita distinguir una operación exitosa de una fallida.

8. La constancia se generará únicamente cuando la revocación haya sido confirmada.

9. Los motivos mostrados en los prototipos representan un catálogo preliminar y pueden requerir ajustes institucionales.

10. La opción Otro requerirá que el ciudadano escriba una explicación.

11. La cancelación será irreversible respecto de los certificados ya revocados.

12. Para volver a utilizar funciones que dependan de los certificados, el ciudadano podría necesitar una nueva emisión o activación. El proceso exacto está pendiente de validación.

13. El portal será utilizado directamente por el ciudadano, sin intervención obligatoria de un operador.

14. Los servicios externos estarán disponibles para ser integrados, aunque sus contratos todavía no hayan sido proporcionados.

15. La constancia podrá descargarse en un formato documental. El formato definitivo está pendiente de validación.

---

## 17. Restricciones y consideraciones

### 17.1. Institucionales

- La solución se desarrolla dentro del contexto de RENIEC.
- Los textos, términos y consecuencias comunicadas deben ser aprobados institucionalmente.
- La operación debe alinearse con las políticas de certificación digital aplicables.
- Las responsabilidades entre áreas institucionales están pendientes de definición.

### 17.2. Seguridad

- La revocación es una operación sensible y potencialmente irreversible.
- Debe impedirse su ejecución sin autenticación válida.
- Deben evitarse solicitudes duplicadas.
- El resultado debe ser trazable.
- El sistema no debe mostrar una revocación exitosa sin confirmación del servicio.
- Debe analizarse el tratamiento de respuestas tardías o inciertas.

### 17.3. Privacidad

- El sistema procesará información de identificación personal.
- Debe limitarse la exposición del DNI y de los datos personales.
- La respuesta de la consulta previa a la autenticación debe revisarse desde la perspectiva de privacidad.
- El periodo de conservación de motivos, evidencias y registros está pendiente de definición.
- Debe evitarse solicitar información confidencial dentro del campo Otro motivo.

### 17.4. Comunicación al ciudadano

- Debe utilizarse lenguaje comprensible.
- No debe indicarse que se revoca la identidad del ciudadano.
- No debe afirmarse que se cancela el DNI.
- No deben comunicarse efectos que el servicio real no garantice.
- Debe diferenciarse claramente entre cancelar el trámite y cancelar los certificados.
- La consecuencia inmediata debe presentarse antes de la confirmación.

### 17.5. Accesibilidad y usabilidad

- El flujo debe ser comprensible para ciudadanos con distintos niveles de conocimiento digital.
- La selección de motivos debe funcionar adecuadamente en diferentes tamaños de pantalla.
- Los mensajes no deben depender únicamente del color.
- Los controles y advertencias deben ser accesibles.
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

---

## 18. Información confirmada

- El sistema será desarrollado para RENIEC.
- El usuario principal será un ciudadano que actúa como persona natural.
- El objeto de la operación son los certificados digitales.
- Los certificados se encuentran relacionados con el DNI del ciudadano.
- El ciudadano ingresará su número de DNI al inicio.
- Un servicio web verificará si el DNI tiene certificados digitales.
- La consulta de certificados se realizará antes de la autenticación con ID Perú.
- Si existen certificados aplicables, el ciudadano continuará con la autenticación.
- ID Perú es un servicio existente y externo que se utilizará para autenticar la identidad.
- ID Perú no es el objeto que se cancela.
- Los certificados se tratarán como un conjunto.
- El ciudadano no seleccionará certificados individuales.
- La operación será una cancelación inmediata.
- Técnicamente, la operación corresponde a una revocación.
- En la interfaz dirigida al ciudadano se recomienda utilizar el término cancelación.
- El ciudadano deberá seleccionar un motivo.
- Se incluirá la opción Otro motivo.
- Al seleccionar Otro, el ciudadano podrá indicar una causa no contemplada.
- La revocación se ejecutará mediante otro servicio web.
- El flujo contemplará una confirmación antes de ejecutar la operación.
- Después del resultado se mostrará una constancia o comprobante.
- El sistema no cancela la identidad civil, el DNI ni la cuenta de ID Perú.
- La operación no corresponde a una renovación.

---

## 19. Aspectos pendientes de validación

### Prioridad alta

1. **Alcance exacto de los certificados**  
   Determinar qué tipos de certificados están incluidos y cómo se relacionan con el DNIe.

2. **Criterio de elegibilidad**  
   Precisar qué significa que un DNI “tenga certificados digitales” y cuáles pueden revocarse.

3. **Respuesta del servicio de consulta**  
   Identificar posibles estados, errores y datos proporcionados.

4. **Respuesta del servicio de revocación**  
   Definir resultados exitosos, fallidos, parciales o inciertos.

5. **Correspondencia entre DNI e identidad autenticada**  
   Confirmar cómo se validará que el usuario autenticado por ID Perú corresponde al DNI ingresado.

6. **Efectos exactos de la revocación**  
   Determinar qué funciones dejarán de estar disponibles y cuáles no se verán afectadas.

7. **Tratamiento de operaciones inciertas**  
   Definir qué hacer cuando el servicio no responde, responde tarde o no permite conocer el resultado.

### Prioridad media

8. **Catálogo definitivo de motivos**  
   Validar nombres, descripciones y posibles códigos institucionales.

9. **Reglas de Otro motivo**  
   Determinar obligatoriedad, longitud, caracteres permitidos y tratamiento de información sensible.

10. **Contenido de la confirmación**  
    Aprobar los textos legales, advertencias y consecuencias.

11. **Constancia o comprobante**  
    Definir nombre oficial, campos, formato, mecanismo de verificación y valor institucional.

12. **Proceso actual**  
    Documentar el procedimiento vigente y los problemas que se espera resolver.

13. **Reintentos**  
    Establecer cuándo puede repetirse una consulta, autenticación o revocación.

14. **Canales de soporte**  
    Definir a dónde se deriva al ciudadano cuando no puede completar el proceso.

### Prioridad complementaria

15. **Reglas de auditoría y trazabilidad.**

16. **Conservación de datos y evidencias.**

17. **Políticas de privacidad.**

18. **Requisitos de accesibilidad.**

19. **Disponibilidad esperada del servicio.**

20. **Responsables institucionales del proceso.**

21. **Mensajes para certificados vencidos o previamente revocados.**

22. **Comportamiento cuando falla la generación de la constancia después de una revocación exitosa.**

---

## 20. Riesgos de interpretación

### 20.1. Confundir certificados digitales con identidad digital

El sistema no elimina ni revoca la identidad de la persona. Utilizar el término identidad digital como objeto de la operación podría generar una comprensión incorrecta del alcance.

### 20.2. Confundir cancelación con revocación

Ambos términos representan perspectivas distintas:

- Cancelación: comunicación ciudadana.
- Revocación: operación técnica.

Usarlos sin una convención puede producir inconsistencias en interfaces, documentos y requisitos.

### 20.3. Confundir DNI con certificados digitales

El DNI se utiliza para identificar al ciudadano e iniciar la consulta. No es el elemento cancelado.

### 20.4. Confundir DNI con DNI electrónico

El sistema solicita el número de DNI, pero los certificados están relacionados con el contexto del DNIe. La relación exacta debe documentarse para evitar términos imprecisos.

### 20.5. Interpretar que el ciudadano elige certificados

Aunque puedan existir varios certificados, el sistema los tratará como un conjunto. No habrá selección individual.

### 20.6. Interpretar ID Perú como la credencial cancelada

ID Perú se utiliza para autenticar al titular. La operación no cancela ID Perú.

### 20.7. Interpretar la cancelación como una solicitud diferida

La operación está planteada como inmediata. No debe diseñarse como un expediente pendiente de aprobación, salvo que posteriormente se informe lo contrario.

### 20.8. Afirmar efectos no confirmados

Revocar certificados no necesariamente implica:

- Eliminar una aplicación.
- Retirar automáticamente un DNI digital.
- Cerrar todas las sesiones.
- Bloquear todos los servicios del Estado.
- Desvincular un dispositivo.
- Cancelar la identidad del ciudadano.

Los mensajes deben limitarse a efectos confirmados.

### 20.9. Confundir ausencia de certificados con certificados ya revocados

El servicio podría distinguir varios estados. No debe asumirse que todos equivalen a “no tiene certificados”.

### 20.10. Confundir constancia generada con revocación ejecutada

La revocación y la generación del comprobante son resultados relacionados, pero conceptualmente distintos. Un fallo al generar el documento no debería alterar el resultado real de la revocación.

---

## 21. Límites del documento

Este documento consolida el contexto funcional y conceptual disponible. No define todavía:

- Requisitos funcionales detallados.
- Requisitos no funcionales completos.
- Casos de uso formales.
- Historias de usuario.
- Criterios de aceptación.
- Arquitectura técnica.
- Lenguajes o frameworks.
- Diseño de base de datos.
- Modelos de datos.
- Contratos de API.
- Firmas de servicios web.
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

El proyecto consiste en un sistema web institucional para el Registro Nacional de Identificación y Estado Civil del Perú, dirigido a ciudadanos que actúan como personas naturales. Su finalidad es permitir la cancelación inmediata de los certificados digitales asociados al DNI cuando exista pérdida, robo, cambio de equipo o número, sospecha de uso no autorizado u otra circunstancia declarada por el titular.

El término recomendado para la interfaz ciudadana es **cancelación de certificados digitales**. Desde el punto de vista técnico, la operación ejecutada corresponde a una **revocación**. El sistema no cancela la identidad civil, el número de DNI, el documento físico, el DNI electrónico ni la cuenta de ID Perú. Tampoco permite visualizar o escoger certificados individuales: todos los certificados digitales aplicables se tratan como una unidad funcional.

El flujo confirmado comienza con el ingreso del número de DNI. A continuación, un servicio web consulta si el DNI tiene certificados digitales susceptibles de revocación. Si no existen, el proceso finaliza. Si existen, el ciudadano debe autenticar su identidad mediante ID Perú, que actúa como servicio externo de autenticación y no como objeto de la cancelación.

Después de autenticarse, el ciudadano selecciona un motivo. El catálogo preliminar comprende robo, pérdida, cambio de equipo o número, sospecha de uso no autorizado y Otro motivo. La opción Otro permite registrar una explicación cuando las alternativas disponibles no representan la situación del usuario.

Antes de ejecutar la operación, el sistema debe presentar sus consecuencias y solicitar una confirmación expresa. Tras la confirmación, un segundo servicio web procesa la revocación inmediata de los certificados. Si la respuesta es exitosa, el sistema muestra el resultado y genera una constancia o comprobante.

Todavía deben validarse los tipos exactos de certificados incluidos, los estados devueltos por los servicios, la correspondencia entre el DNI ingresado y la identidad autenticada, los efectos precisos de la revocación, el catálogo definitivo de motivos, el contenido de la constancia, el manejo de resultados inciertos, el proceso vigente, las reglas de auditoría, las políticas de privacidad y el marco normativo aplicable.

---

## 23. Contexto compacto para prompts

> Proyecto web de RENIEC para que personas naturales cancelen inmediatamente los certificados digitales asociados a su DNI. El ciudadano ingresa su DNI; un servicio verifica si tiene certificados revocables; luego se autentica mediante ID Perú, selecciona un motivo —robo, pérdida, cambio de equipo o número, sospecha de uso no autorizado u Otro—, revisa las consecuencias y confirma. Un segundo servicio ejecuta la revocación y el sistema genera una constancia. Los certificados se tratan como un conjunto y no pueden seleccionarse individualmente. En la interfaz debe utilizarse “cancelación de certificados digitales”; técnicamente la operación es una revocación. No se cancela la identidad civil, el DNI, el DNIe ni ID Perú. Permanecen pendientes los contratos de servicios, estados, efectos exactos, contenido de la constancia, reglas de auditoría, privacidad y normativa aplicable.
