## 1. Dependencia y exposición por ambiente

- [x] 1.1 Sustituir `springdoc-openapi-starter-webmvc-api` por `springdoc-openapi-starter-webmvc-ui` 3.0.3 en `backend/pom.xml`, comprobar que no coexistan ambos starters y resolver el árbol Maven sin dependencias adicionales innecesarias.
- [x] 1.2 Mantener OpenAPI y Swagger UI deshabilitados en la configuración común; habilitar `/v3/api-docs`, `/v3/api-docs.yaml` y la ruta documentada de Swagger UI solo en `application-local.yml`.
- [x] 1.3 Configurar `application-test.yml` para generar OpenAPI sin exponer normalmente Swagger UI y preparar la activación aislada de la interfaz en su prueba específica.
- [x] 1.4 Incluir únicamente `/api/v1/**` y `/actuator/health` en la documentación, conservando como único endpoint Actuator públicamente expuesto el health sin detalles internos.

## 2. Metadata, operaciones y modelos OpenAPI

- [x] 2.1 Completar `OpenApiConfiguration` con título, descripción, versión, información institucional y etiquetas ordenadas, sin declarar servidores productivos ni esquemas de seguridad inexistentes.
- [x] 2.2 Completar la documentación de `GET /api/v1/system/status` con finalidad, etiqueta, operación, respuesta 200, error 503, contenido JSON, timestamp UTC y cabecera `X-Correlation-ID` según el comportamiento real.
- [x] 2.3 Completar la documentación de `POST /api/v1/cancellation-requests` con finalidad, cuerpo requerido, resultados normalizados, cabecera de correlación y respuestas 200, 400, 409, 415, 502, 503 y 504 que realmente produce la implementación.
- [x] 2.4 Incorporar `GET /actuator/health` al contrato bajo la responsabilidad técnica correspondiente, documentando su respuesta agregada sin habilitar detalles, componentes ni otros endpoints Actuator.
- [x] 2.5 Documentar `StartCancellationRequest`, `CancellationRequestResponse`, `SystemStatusResponse`, `ApiError` y enumeraciones referenciadas con campos requeridos, descripción, formatos, restricciones reales y ejemplos exclusivamente ficticios o enmascarados.
- [x] 2.6 Revisar el documento generado para eliminar rutas internas, controladores de prueba, detalles de MySQL, payloads de proveedores, DNI completos y cualquier definición anticipada de autenticación o autorización.

## 3. Verificación automatizada de documentación

- [x] 3.1 Añadir una prueba que obtenga `/v3/api-docs` y compruebe las tres operaciones actuales, sus métodos HTTP, etiquetas, summaries, descriptions, operation IDs, contenidos y esquemas referenciados.
- [x] 3.2 Verificar mediante pruebas las validaciones y obligatoriedad del DNI, los campos y formatos de DTO, la cabecera `X-Correlation-ID`, y todos los códigos de éxito y error declarados por cada operación.
- [x] 3.3 Añadir una comparación entre las rutas de aplicación expuestas por Spring MVC y las rutas documentadas, con filtros explícitos para infraestructura y pruebas, además de una aserción separada para `/actuator/health`.
- [x] 3.4 Añadir una prueba aislada que habilite la configuración local de Swagger UI y compruebe que su ruta carga o redirige correctamente hacia la interfaz que consume `/v3/api-docs`.
- [x] 3.5 Añadir aserciones negativas que garanticen la ausencia de rutas test-only, endpoints Actuator no expuestos, esquemas de seguridad, secretos, DNI completos y detalles internos en el contrato.

## 4. Contratos derivados y documentación para desarrolladores

- [x] 4.1 Regenerar el snapshot OpenAPI y los tipos TypeScript mediante los comandos existentes, revisar el cambio generado y confirmar que no se editaron manualmente los artefactos derivados.
- [x] 4.2 Actualizar `backend/README.md` con las URLs JSON, YAML y Swagger UI, perfiles, requisitos de MySQL, forma de ejecutar operaciones actuales y límites de exposición y seguridad.
- [x] 4.3 Registrar en la guía de desarrollo que todo endpoint nuevo o modificado debe actualizar operaciones, DTO, validaciones, códigos HTTP, errores, seguridad implementada, snapshot y pruebas OpenAPI dentro del mismo incremento.

## 5. Validación final

- [x] 5.1 Ejecutar las pruebas rápidas y de documentación del backend y corregir cualquier divergencia entre anotaciones, documento OpenAPI y comportamiento HTTP.
- [x] 5.2 Ejecutar `mvnw.cmd verify` con el runtime de contenedores disponible para confirmar compilación, Flyway, MySQL, endpoints y pruebas existentes.
- [x] 5.3 Ejecutar en el frontend la comprobación de contratos, pruebas y build para confirmar que los tipos sincronizados continúan siendo consumibles.
- [x] 5.4 Levantar MySQL y el backend con perfil local, abrir Swagger UI y comprobar manualmente que las operaciones actuales pueden explorarse y, cuando corresponde, ejecutarse sin documentación de seguridad inexistente.
