## Context

El backend usa Spring Boot 4.1.0 y Java 21. Ya incorpora springdoc 3.0.3 mediante `springdoc-openapi-starter-webmvc-api`, publica el contrato en `/v3/api-docs` solo bajo perfiles local y test, limita el escaneo a `/api/v1/**`, y contiene una configuración `OpenAPI` básica. Los controladores `SystemStatusController` y `CancellationRequestController` tienen anotaciones parciales, algunos DTO poseen `@Schema`, y el frontend conserva un snapshot OpenAPI con tipos generados.

La situación actual permite sincronización máquina-a-máquina, pero no ofrece Swagger UI, no garantiza que cada operación tenga descripción completa y no incluye el único endpoint Actuator expuesto (`/actuator/health`). Tampoco existe una prueba general que compare todas las rutas HTTP relevantes contra la documentación generada. El cambio debe reforzar esta base sin alterar lógica funcional, contratos externos, persistencia o seguridad.

## Goals / Non-Goals

**Goals:**

- Proporcionar Swagger UI funcional y estable en el entorno local.
- Mantener OpenAPI como descripción generada desde la implementación real.
- Documentar todas las rutas actualmente expuestas: las operaciones de `/api/v1/**` y `/actuator/health`.
- Organizar las operaciones entre API ciudadana y operación técnica.
- Describir DTO, validaciones, cabeceras de correlación, éxitos y errores reales sin datos sensibles.
- Incorporar pruebas que fallen cuando una ruta nueva o modificada no esté representada correctamente en OpenAPI.
- Mantener sincronizados el snapshot OpenAPI y los tipos TypeScript existentes.

**Non-Goals:**

- Implementar o documentar JWT, OAuth2, autenticación, autorización o cookies de sesión inexistentes.
- Añadir endpoints, alterar reglas funcionales o modificar el modelo de datos.
- Exponer Swagger UI por defecto ni definir su política productiva definitiva.
- Introducir un archivo OpenAPI manual paralelo, otro generador, portal externo o SDK.
- Documentar controladores exclusivos de pruebas o endpoints Actuator no expuestos.

## Decisions

### 1. Reutilizar springdoc 3.0.3 y cambiar al starter con interfaz

Se reemplazará `springdoc-openapi-starter-webmvc-api` por `springdoc-openapi-starter-webmvc-ui` conservando la versión 3.0.3. La documentación oficial de springdoc identifica esta variante como la integración WebMVC con Swagger UI para Spring Boot 4. El starter UI ya incluye la generación OpenAPI utilizada actualmente, por lo que ambos starters no deben coexistir.

Alternativas descartadas:

- Mantener el starter API-only y servir una página Swagger manual: duplica configuración y recursos.
- Añadir Springfox u otra biblioteca: introduce una segunda estrategia y peor alineación con la base actual.
- Mantener solamente el JSON: no satisface la exploración y ejecución solicitadas.

### 2. Exposición limitada por perfil

La configuración común mantendrá `springdoc.api-docs.enabled=false` y añadirá `springdoc.swagger-ui.enabled=false`. El perfil `local` habilitará ambos recursos y fijará una ruta documentada, preferentemente `/swagger-ui.html`. El perfil `test` habilitará el documento JSON para las verificaciones automatizadas, pero mantendrá la UI deshabilitada salvo en la prueba específica que la active mediante propiedades aisladas. Producción continuará fuera de alcance.

Esta decisión evita convertir una herramienta de desarrollo en una superficie pública accidental.

### 3. Un contrato generado con organización por etiquetas

El contrato conservará `/api/v1/**` como API de aplicación y añadirá únicamente `/actuator/health` como operación técnica expuesta. Las etiquetas serán comprensibles y estables, por ejemplo:

- `Solicitudes de cancelación`: inicio y elegibilidad de la solicitud ciudadana.
- `Estado técnico`: disponibilidad del backend, MySQL y health operativo.

Swagger UI mostrará estas operaciones en un único documento para evitar múltiples selectores y configuración innecesaria en un backend pequeño. Las rutas de infraestructura de Swagger/OpenAPI no se documentarán como operaciones de negocio.

Alternativa descartada: crear varios grupos OpenAPI para solo tres operaciones; agregaría navegación y pruebas sin aportar claridad al MVP.

### 4. Anotaciones próximas al contrato real

Los controladores conservarán `@Operation`, `@ApiResponses`, `@Tag`, `@Header` y esquemas de contenido donde el comportamiento sea específico de la operación. Los DTO usarán `@Schema` junto con las restricciones Jakarta Validation reales. Springdoc seguirá infiriendo tipos y restricciones desde el código; las anotaciones completarán significado, ejemplos ficticios, formatos y respuestas que no pueden inferirse con precisión.

No se crearán DTO exclusivos para Swagger ni ejemplos con DNI reales. `ApiError` será el esquema común de errores y las operaciones declararán solamente estados que el controlador, filtros o `GlobalExceptionHandler` puedan producir.

### 5. Sin esquemas de seguridad anticipados

La configuración OpenAPI no definirá `SecurityScheme`, requisitos de seguridad ni cabeceras Authorization. Cuando JWT exista, su cambio deberá ampliar la especificación, Swagger UI y las pruebas en el mismo incremento.

### 6. Prueba de cobertura documental como regla ejecutable

Una prueba de integración obtendrá el documento generado y comparará sus operaciones con las rutas de aplicación expuestas por Spring MVC, filtrando infraestructura interna y controladores de prueba. Verificará además `/actuator/health`, etiquetas, descripciones, contenido, códigos HTTP, cabecera `X-Correlation-ID`, esquemas y restricciones clave.

Las pruebas específicas de cada endpoint seguirán verificando el comportamiento HTTP real. La comparación general establecerá el mínimo obligatorio para futuros endpoints; al modificar una operación, sus aserciones contractuales y snapshot deberán actualizarse.

### 7. Mantener el snapshot frontend como derivado del backend

Después de completar anotaciones y configuración se regenerarán el snapshot OpenAPI y `generated.ts` mediante los comandos existentes. No se editarán manualmente tipos generados. La documentación visual y el contrato consumido por TypeScript compartirán la misma fuente.

## Risks / Trade-offs

- [Swagger UI puede exponerse accidentalmente fuera del entorno local] → mantener ambos recursos deshabilitados en configuración común y probar la activación por perfil.
- [Las anotaciones pueden divergir del comportamiento] → combinar inferencia desde DTO/Validation con pruebas HTTP y aserciones sobre el documento generado.
- [Incluir Actuator puede revelar detalles] → incluir solo `/actuator/health`, conservar `show-details=never` y comprobar ausencia de información interna.
- [La prueba de inventario puede capturar rutas internas] → filtrar explícitamente infraestructura de Spring, Swagger y controladores de prueba, y comparar solo rutas de aplicación más el health expuesto.
- [Ejemplos OpenAPI pueden revelar datos sensibles] → usar exclusivamente valores ficticios y DTO de respuesta enmascarados; nunca incluir DNI completos reales, credenciales o detalles de base de datos.
- [Más anotaciones aumentan el mantenimiento] → limitarse a operaciones y DTO actuales, reutilizar `ApiError` y evitar abstracciones de anotaciones hasta que exista repetición suficiente.

## Migration Plan

1. Cambiar el starter Maven y ajustar configuración común, local y test.
2. Completar metadata, etiquetas, operaciones, respuestas y esquemas.
3. Añadir pruebas del documento y de Swagger UI.
4. Regenerar el snapshot y los tipos TypeScript.
5. Actualizar el README y ejecutar compilación, pruebas y verificación completa.

El rollback consiste en restaurar el starter API-only, retirar la configuración de Swagger UI y conservar el contrato JSON anterior; no hay migraciones de datos ni cambios de API funcional.

## Open Questions

- La exposición de Swagger UI y `/v3/api-docs` en producción seguirá pendiente de una decisión de despliegue y seguridad posterior.
- La documentación de seguridad se definirá únicamente cuando se implemente JWT y exista un contrato de sesión aprobado.
