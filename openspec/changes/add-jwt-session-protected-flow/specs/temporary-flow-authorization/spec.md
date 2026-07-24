## REMOVED Requirements

### Requirement: Pre-authentication continuation is bound to the current browser
**Reason**: La sesión transaccional se crea ahora después de disponibilidad positiva y representa desde ese momento la única continuidad autorizada.

**Migration**: Sustituir la cookie con propósito `IDENTITY_INIT` por los access y refresh cookies de `citizen-flow-session`.

### Requirement: Successful identity rotates into temporary flow authorization
**Reason**: ID Perú actualizará la misma sesión existente en vez de emitir una segunda autorización específica del intento.

**Migration**: Tras coincidencia de identidad, actualizar el estado de solicitud y sesión y rotar las cookies de sesión cuando corresponda.

### Requirement: Authorization cookie uses secure browser attributes
**Reason**: Los atributos de las cookies quedan definidos de forma completa para access y refresh dentro de `citizen-flow-session`.

**Migration**: Reemplazar la cookie temporal única por las dos cookies de sesión con ámbitos y vigencias independientes.

### Requirement: Protected access validates token and persisted state
**Reason**: La validación persistida pasa a ser una responsabilidad general de la sesión y de la protección del flujo, no del intento ID Perú.

**Migration**: Mover la validación de propósito/jti almacenado en `identity_verification` al principal de sesión y a `cancellation_flow_session`.

### Requirement: Authorization ends without creating a recoverable session
**Reason**: SPEC-13 requiere expresamente una sesión persistida con refresh token para la operación actualmente activa.

**Migration**: Conservar la prohibición de recuperar trámites históricos dentro de `citizen-flow-session`, pero permitir renovación limitada de la sesión activa.
