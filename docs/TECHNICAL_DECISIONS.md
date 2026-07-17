# Decisiones técnicas iniciales

Este documento registra la base técnica acordada para futuras etapas. No configura ni implementa todavía ninguna parte del sistema. El contexto funcional completo se encuentra en [`context/PROJECT_CONTEXT.md`](./context/PROJECT_CONTEXT.md).

## Tecnologías y estructura

- Backend con Spring Boot.
- Frontend con Next.js.
- Base de datos MySQL.
- Estilos con Tailwind CSS.
- Dos carpetas principales futuras en la raíz del repositorio: `/backend` y `/frontend`.

## Sesión y progreso

- Se utilizará JWT para la sesión y la comunicación autenticada entre frontend y backend.
- El progreso del proceso se persistirá en el backend y en MySQL.
- El ciudadano podrá recuperar su progreso desde otro navegador o dispositivo después de verificar nuevamente su identidad.
- Para el MVP, la recuperación busca por DNI la solicitud sin finalizar y continúa desde `request_status`, sin fecha de expiración.
- JWT se diseñará por separado y no implica una tabla de sesiones, refresh tokens ni registros por navegador o dispositivo.

## Principios de arquitectura y datos

- La arquitectura será sencilla, incremental y fácil de mantener.
- La base de datos se diseñará sin sobreingeniería.
- No se creará una tabla por cada pantalla, paso o estado del flujo.
- No se utilizarán microservicios, colas, event sourcing, CQRS ni patrones complejos sin una necesidad comprobada.

## Integraciones y alcance

- Las integraciones externas se definirán mediante interfaces y usarán mocks reemplazables mientras no existan contratos oficiales.
- No se inventarán contratos para ID Perú, la consulta de certificados ni la revocación.
- No se incluirán módulos administrativos ni funcionalidades fuera del flujo ciudadano.

Los modelos definitivos, contratos externos y detalles de implementación se decidirán en cambios posteriores cuando exista información validada.
