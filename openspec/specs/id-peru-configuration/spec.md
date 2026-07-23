# id-peru-configuration Specification

## Purpose
TBD - created by archiving change simplify-id-peru-configuration. Update Purpose after archive.
## Requirements
### Requirement: Configuración externa mínima de ID Perú
El backend SHALL requerir en modo real únicamente las URLs base de frontend y backend, la URI base de ID Perú, las credenciales institucionales, el referer autorizado y un secreto maestro del flujo.

#### Scenario: Inicio en modo real con configuración completa
- **WHEN** el backend inicia en modo real con todos los valores mínimos válidos
- **THEN** la integración ID Perú queda disponible sin requerir variables para rutas, timeouts, vigencias, nombre de cookie ni caché

#### Scenario: Falta un valor obligatorio en modo real
- **WHEN** el backend inicia en modo real sin una credencial, URI base, referer, URL de aplicación o secreto maestro obligatorio
- **THEN** el inicio falla con un mensaje que identifica la propiedad faltante sin revelar secretos

### Requirement: URI del flujo derivadas por la aplicación
El backend SHALL derivar los endpoints v2 de ID Perú desde una URI base institucional y SHALL construir callback y retorno desde las URLs base de las aplicaciones y rutas canónicas controladas por el código.

#### Scenario: Construcción del flujo real
- **WHEN** se inicia una autenticación ID Perú en modo real
- **THEN** autorización, token, userinfo, JWKS, callback y retorno utilizan las rutas derivadas documentadas

#### Scenario: URI base insegura
- **WHEN** el modo real recibe una URI institucional o URL de aplicación que no cumple las restricciones HTTPS aplicables
- **THEN** el backend rechaza la configuración antes de atender solicitudes

### Requirement: Decisiones técnicas internas
El backend SHALL mantener timeouts, vigencias, caché JWKS, nombre de cookie y mecanismo de autenticación inicial como valores internos únicos, sin exigir variables de entorno para el MVP.

#### Scenario: Ejecución sin ajustes técnicos externos
- **WHEN** el backend inicia con la configuración mínima del ambiente
- **THEN** utiliza los valores internos documentados para timeouts, vigencias, caché, cookie y `acr_values`

#### Scenario: Propiedad futura sin necesidad comprobada
- **WHEN** se evalúa añadir un nuevo ajuste externo de ID Perú
- **THEN** se incorpora únicamente si existe una diferencia real entre ambientes o una necesidad operativa documentada

### Requirement: Secreto maestro con claves derivadas
El backend SHALL aceptar un único secreto maestro Base64 de 32 bytes y SHALL derivar claves criptográficamente separadas para cifrar PKCE y firmar la continuidad del flujo.

#### Scenario: Derivación determinista y separada
- **WHEN** se configura un secreto maestro válido
- **THEN** las claves derivadas son estables para el mismo secreto y diferentes entre los propósitos de cifrado y firma

#### Scenario: Secreto maestro inválido
- **WHEN** el secreto no es Base64 válido o no representa exactamente 32 bytes
- **THEN** el backend falla al iniciar sin registrar el valor recibido

### Requirement: Separación por perfiles
El backend SHALL seleccionar `disabled`, `mock` o `real` mediante perfiles del ambiente y SHALL evitar que el modo mock necesite configuración institucional real.

#### Scenario: Desarrollo local
- **WHEN** se inicia el perfil local sin credenciales institucionales
- **THEN** el adaptador mock funciona con valores locales seguros y un escenario determinista configurable

#### Scenario: Producción
- **WHEN** se inicia el perfil productivo
- **THEN** el backend exige el adaptador real, cookies seguras y la configuración externa mínima

### Requirement: Ausencia de regresión funcional y de seguridad
La simplificación SHALL conservar PKCE S256, validación de `state`, validación criptográfica de JWT y JWKS, correspondencia del DNI, cookies HttpOnly y ausencia de tokens o secretos en logs y respuestas.

#### Scenario: Flujo válido después de simplificar
- **WHEN** un ciudadano completa una autenticación válida
- **THEN** el resultado funcional y la autorización temporal son equivalentes al comportamiento anterior

#### Scenario: Flujo inválido después de simplificar
- **WHEN** se recibe `state`, código, token, firma, issuer, audiencia o identidad inválida
- **THEN** el backend conserva el rechazo controlado correspondiente y no permite continuar

### Requirement: Documentación enfocada en operación
La documentación SHALL mostrar solo las variables vigentes, los valores internos relevantes y la migración desde los nombres retirados.

#### Scenario: Preparación de un ambiente
- **WHEN** un desarrollador u operador consulta la documentación de ID Perú
- **THEN** encuentra un único listado mínimo sin propiedades obsoletas o duplicadas

