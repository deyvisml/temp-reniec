# Decisiones técnicas iniciales

Este documento registra la base técnica acordada para futuras etapas. No configura ni implementa todavía ninguna parte del sistema. El contexto funcional completo se encuentra en [`context/PROJECT_CONTEXT.md`](./context/PROJECT_CONTEXT.md).

## Contexto funcional vigente y alineación

- `docs/context/PROJECT_CONTEXT.md` es la única fuente funcional vigente y prevalece sobre documentos técnicos o diseños que todavía describan el flujo anterior.
- La consulta inicial tiene como objetivo devolver una lista de emisiones vigentes, presentadas al ciudadano como certificados digitales vigentes únicamente después de autenticar su identidad.
- El flujo permite seleccionar uno, varios o todos los certificados disponibles. Después de confirmar, esa selección es inmutable y forma el conjunto atómico de revocación; los no seleccionados permanecen fuera de la operación.
- La integración de revocación deberá enviar la lista completa bajo una única clave de idempotencia y aceptar solo un resultado común: exitoso, fallido o incierto. Un proveedor que produzca resultados mixtos por UUID será incompatible y no se normalizará como resultado parcial.
- `revocation_operation.normalized_result` será la fuente técnica del resultado; no se persistirán filas de resultado por certificado. La constancia identificará el conjunto seleccionado y su resultado común.
- Las adaptaciones de contratos, persistencia y vistas se realizarán mediante cambios funcionales posteriores, sin anticipar contratos institucionales externos aún no confirmados.

## Tecnologías y estructura

- Backend con Spring Boot.
- Frontend con Next.js.
- Base de datos MySQL.
- Estilos con Tailwind CSS.
- Dos carpetas principales futuras en la raíz del repositorio: `/backend` y `/frontend`.

## Sesión y progreso

- Se utilizará JWT para la sesión y la comunicación autenticada entre frontend y backend.
- El progreso del proceso se persistirá en el backend y en MySQL.
- `request_status` controla las transiciones y conserva la trazabilidad de la solicitud actual; no se utilizará para restaurar una solicitud cuando el ciudadano vuelva a la página de inicio.
- Cada nuevo ingreso del DNI desde inicio creará una solicitud y una consulta nuevas. Una solicitud anterior no confirmada podrá quedar `ABANDONED` y las solicitudes terminales permanecerán como historial.
- No se recuperarán automáticamente selecciones, resultados ni constancias anteriores. Una eventual consulta histórica de constancias será un caso de uso independiente y autenticado.
- Una revocación confirmada en curso o con resultado incierto podrá bloquear otro inicio para proteger la idempotencia, sin devolver ni reabrir el trámite anterior.
- Un resultado incierto se reconciliará conservando la misma operación y clave de idempotencia; no habilitará automáticamente otra ejecución.
- JWT se diseñará por separado y no implica una tabla de sesiones, refresh tokens, registros por navegador o dispositivo ni recuperación del progreso.

## Principios de arquitectura y datos

- La arquitectura será sencilla, incremental y fácil de mantener.
- La base de datos se diseñará sin sobreingeniería.
- No se creará una tabla por cada pantalla, paso o estado del flujo.
- El esquema efectivo conserva una tabla de certificados asociados a la solicitud y una operación técnica global; no requiere una tabla de resultados individuales porque la regla es todos o ninguno.
- No se utilizarán microservicios, colas, event sourcing, CQRS ni patrones complejos sin una necesidad comprobada.

## Integraciones y alcance

- Las integraciones externas se definirán mediante interfaces y usarán mocks reemplazables mientras no existan contratos oficiales.
- No se inventarán contratos para ID Perú, la consulta de certificados ni la revocación.
- No se incluirán módulos administrativos ni funcionalidades fuera del flujo ciudadano.

Los modelos definitivos, contratos externos y detalles de implementación se decidirán en cambios posteriores cuando exista información validada.
