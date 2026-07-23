## Context

La pantalla inicial crea una solicitud nueva y, cuando el primer servicio confirma disponibilidad, la deja en `PENDING_IDENTITY_VERIFICATION`. El frontend intenta continuar a `/verificacion-identidad`, pero esa ruta y sus APIs todavía no existen. El identificador numérico de la solicitud es informativo y no constituye autorización.

La fuente técnica principal es **IDAAS V2 – Especificaciones Técnicas – Idaas2-Web**, versión 1.2, aprobada el 22/05/2026. El documento define OAuth 2.0/OIDC Authorization Code, callback `POST`, PKCE `S256`, `state`, cifrado `vd`, `/token`, `/userinfo` y `/jwks`. La referencia visual conserva un stepper antiguo de cuatro pasos; la aplicación implementará el flujo vigente de cinco pasos documentado en `PROJECT_CONTEXT.md`.

También se revisó `C:\FastFolder\sistema-autorizacion-certificados-reniec`. Son reutilizables conceptualmente su separación por proveedor, configuración tipada, cliente HTTP con timeout, hash de `state`, cifrado de `vd`, tratamiento del header `Referer` y validación de JWT/JWKS. Se adaptarán a este monolito y al modelo `identity_verification`. Se descartan el soporte v1, la sesión de autorización de ese dominio, su circuit breaker, el callback GET de compatibilidad y la reconstrucción determinista del `code_verifier`: esta especificación exige un secreto aleatorio por intento y no justifica esas piezas adicionales.

No hay credenciales institucionales confirmadas para este proyecto. Por ello, “integración real completa” significa que el adaptador implementará íntegramente el contrato v1.2 y se probará contra un servidor ID Perú controlado; la validación contra el ambiente institucional será una comprobación manual condicionada a convenio, `client_id`, `client_secret`, URLs registradas y autorización de uso.

## Goals / Non-Goals

**Goals:**

- Autenticar al ciudadano con ID Perú v2 antes de habilitar el listado detallado.
- Validar estado, PKCE, callback, tokens, JWKS, datos de ciudadano y coincidencia de DNI en backend.
- Ofrecer modo real y modo simulado con la misma orquestación y estados persistidos.
- Proteger la continuidad del navegador con cookies cortas, `HttpOnly` y sin datos personales.
- Mantener una persistencia sencilla sobre `identity_verification`, sin tabla de sesiones.
- Implementar el paso 1 accesible, responsive y coherente con el flujo de cinco pasos.

**Non-Goals:**

- Implementar listado y selección, motivo, confirmación, revocación o constancia.
- Crear una sesión permanente, refresh tokens, recuperación de trámites o un servidor OAuth propio.
- Soportar ID Perú v1, varios `acr_values` simultáneos o mecanismos no habilitados por el convenio.
- Persistir tokens, códigos, biometría, fotografías, respuestas completas o claims innecesarios.
- Implementar circuit breaker, reintentos generales o una plataforma de gestión de secretos.

## Decisions

### 1. El PDF v1.2 será la fuente contractual local

Durante la implementación se copiará el binario sin modificar a `docs/integrations/id-peru/IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf` y se registrarán versión, fecha, estado aprobado, SHA-256 y áreas dependientes en el README contiguo. La documentación no reproducirá credenciales ni ejemplos sensibles del PDF.

**Alternativa descartada:** mantener el PDF solo en Descargas. Impediría que futuras tareas validen el protocolo desde una ruta estable y versionada.

### 2. Un caso de uso común y adaptadores seleccionados por configuración

La orquestación dependerá de un puerto `CitizenIdentityProviderPort`. `IdPeruV2Adapter` construirá la autorización y realizará `/token`, `/userinfo` y `/jwks`; `SimulatedIdPeruAdapter` reproducirá resultados deterministas. `app.id-peru.mode=real|mock` seleccionará el adaptador. `prod` solo aceptará `real`; `local` usará `mock` por defecto y podrá activar `real` con configuración completa.

El simulador expondrá únicamente rutas de proveedor bajo perfil local/test y completará el mismo callback backend, por lo que no omitirá validación de `state`, uso único, expiración, coincidencia ni emisión de autorización.

**Alternativa descartada:** colocar condiciones mock dentro del controlador o frontend. Crearía dos flujos de negocio y permitiría que las pruebas ignorasen controles importantes.

### 3. Continuidad previa a la autenticación mediante cookie firmada corta

Cuando la disponibilidad sea positiva, el backend emitirá una cookie de continuidad `HttpOnly` con propósito `IDENTITY_INIT`, identificador de solicitud, `iat`, `exp`, audiencia y `jti`, pero sin DNI. La firma usará una clave externa. El frontend navegará a `/verificacion-identidad` sin DNI ni `requestId` en la URL. El endpoint de inicio resolverá la solicitud desde la cookie y comprobará estado y disponibilidad persistidos.

Esto no crea una sesión recuperable: la cookie pertenece al navegador actual, expira pronto y no permite reabrir solicitudes finalizadas. Si falta, la vista orientará a iniciar una consulta nueva.

**Alternativa descartada:** aceptar libremente `requestId`. Los IDs son secuenciales y permitirían iniciar una autenticación para una solicitud ajena.

### 4. Endpoints versionados y retorno fijo

Se incorporarán como mínimo:

- `POST /api/v1/identity-verifications` para preparar el intento y devolver `authorizationUrl`.
- `POST /api/v1/identity-verifications/callback` como `redirect_uri` registrado y receptor de `code`, `state`, `session_state` o `error` en formulario.
- `GET /api/v1/identity-verifications/current` para que la vista conozca el estado asociado a su cookie.
- `POST /api/v1/identity-verifications/logout` para invalidar la autorización local y limpiar la cookie.

El callback nunca devolverá tokens al frontend. Tras procesarlo, responderá `303` hacia una ruta fija `/verificacion-identidad/retorno`; el resultado se consultará por cookie y no mediante detalles sensibles en query string. OpenAPI documentará inicio, estado y salida; el callback se documentará como operación técnica de proveedor y no se ofrecerá como prueba interactiva ordinaria.

### 5. `state` aleatorio, opaco, almacenado solo por hash y consumido atómicamente

Cada intento generará al menos 256 bits aleatorios mediante `SecureRandom`, los codificará en Base64 URL-safe sin padding y enviará el valor según la codificación exigida por ID Perú. La base guardará SHA-256 del valor, una restricción única, expiración y fecha de consumo. El callback localizará el intento por el hash y hará una actualización condicional `STARTED + not consumed + not expired`; solo un callback podrá consumirlo.

Un callback con `error` también consumirá el intento cuando el `state` sea válido. `session_state` se validará por presencia, longitud y caracteres establecidos, pero no se comparará con un valor previo inexistente ni se reinterpretará como el `state` de la aplicación.

**Alternativa descartada:** guardar `state` en texto plano o depender de una sesión HTTP en memoria. La primera opción aumenta exposición; la segunda no funciona de forma estable entre reinicios o instancias.

### 6. PKCE aleatorio y recuperable solo durante el intento

Por intento se generará un `code_verifier` criptográficamente aleatorio de 64 caracteres válidos. `code_challenge` será `BASE64URL(SHA256(ASCII(code_verifier)))` sin padding y se enviará con `code_challenge_method=S256`.

El intercambio necesita recuperar el verifier después de la redirección. Por eso se guardará temporalmente cifrado con AES-GCM mediante una clave de aplicación externa distinta de las credenciales ID Perú; nunca se guardará en claro ni solo como hash. El ciphertext se eliminará al consumir el callback, tanto en éxito como en error. Una rotación de esa clave deberá respetar intentos aún vigentes o invalidarlos de forma controlada.

**Alternativa descartada:** reconstruirlo por HMAC como hace la referencia. Aunque evita ciphertext, deja de ser un valor aleatorio independiente por operación y acopla su seguridad a datos persistidos y a una fórmula propia.

### 7. `vd` replica exactamente el contrato y queda encapsulado

Para `face_mobile` y cualquier mecanismo que lo requiera, un componente aislado obtendrá KEY e IV de los primeros 16 caracteres UTF-8 del `client_id`, cifrará el DNI con `AES/CBC/PKCS5Padding`, codificará Base64 y permitirá que el constructor URI aplique percent-encoding una sola vez. Sus pruebas usarán vectores conocidos y verificarán que el DNI no aparezca en URL ni logs.

`acr_values` será un único valor validado contra `face_mobile`, `two_factor_mobile`, `pki_dnie` o `pki_token`. Inicialmente `local` usará `face_mobile`, consistente con la vista, pero el ambiente real deberá declarar el valor habilitado en su convenio. `vd` se incluirá solo cuando el mecanismo elegido lo exija según el PDF.

### 8. Cliente real estricto y configuración fail-closed

`IdPeruProperties` externalizará `client-id`, `client-secret`, `auth-uri`, `token-uri`, `userinfo-uri`, `logout-uri`, `jwks-uri`, `issuer`, `redirect-uri`, `frontend-return-uri`, `acr-values`, `max-age`, `referer`, timeouts, TTLs y claves internas. Los endpoints reales exigirán HTTPS y host permitido; tests podrán usar HTTP local explícito.

El token se solicitará por `POST application/x-www-form-urlencoded` con `grant_type=authorization_code`, código, redirect URI exacta, cliente, secreto, verifier y `Referer`. `/userinfo` se invocará por `POST`, Bearer y `Referer`. No habrá reintento automático de `/token`, porque código y verifier son de un solo uso. Timeouts, HTTP no exitoso y cuerpos inválidos producirán errores separados.

La ausencia de configuración obligatoria impedirá iniciar el adaptador real. `.env.example` contendrá nombres y placeholders, nunca secretos reales.

### 9. Validación JOSE mediante una biblioteca mantenida

Se añadirá la dependencia mínima administrada por Spring Boot para Nimbus JOSE/JWT, evitando una implementación RSA/JWT manual. El `id_token` se aceptará solo con algoritmo permitido (`RS256` inicialmente), `kid` conocido, firma válida, `exp`/`nbf`, emisor configurado, audiencia `client_id`, subject y claims obligatorios.

El JWT contenido en `/userinfo` se validará antes de leer `sub`, `doc` y, si se requiere en el futuro, `first_name`. Se exigirá firma, algoritmo, clave, vigencia y audiencia; además, el `sub` deberá ser coherente con el `id_token`. Solo `sub` y `doc` se usarán en este incremento; el primer nombre no se persistirá porque la pantalla actual no lo necesita.

JWKS tendrá caché en memoria con TTL configurable. Un `kid` ausente provocará un único refresh inmediato para soportar rotación; si continúa ausente, se rechazará el token. No se confiará en un JWT por poder decodificarlo.

**Alternativa descartada:** copiar el validador RSA manual de la referencia. Aumentaría código criptográfico propio y omitiría validaciones OIDC completas.

### 10. Coincidencia del DNI y transiciones controladas

Después de validar tokens y userinfo, el backend comparará `doc` con el DNI de la solicitud en tiempo constante razonable para cadenas normalizadas de ocho dígitos. Coincidencia válida: intento `VERIFIED`, solicitud `IDENTITY_VERIFIED`, auditoría y autorización temporal. Diferencia: intento `IDENTITY_MISMATCH`, solicitud sin acceso al paso 2, sin revelar el documento autenticado. Cancelación, rechazo, expiración, timeout, indisponibilidad y respuesta inválida tendrán códigos normalizados y no consultarán el segundo servicio.

No se persistirá `doc`; se guardará un hash del `sub` como referencia técnica cuando esté disponible y el resultado `MATCHED`/`MISMATCHED`.

### 11. Autorización posterior rotada y validada contra base de datos

El callback exitoso reemplazará la cookie `IDENTITY_INIT` por una cookie firmada con propósito `FLOW_AUTH`, solicitud, intento verificado, `jti`, `iat`, `exp` y audiencia, sin PII. `HttpOnly`, `Path=/`, `SameSite=Lax` y `Secure=true` bajo HTTPS serán obligatorios; local podrá desactivar `Secure` explícitamente.

La base almacenará únicamente el hash de `jti`, vigencia e invalidación en el intento verificado. Cada acceso futuro comprobará firma, propósito, expiración, coincidencia del hash, estado `IDENTITY_VERIFIED` o posterior permitido y ausencia de invalidación. Logout, abandono, expiración o finalización invalidarán el registro y limpiarán la cookie. No habrá tabla de sesiones, refresh token ni recuperación desde otro navegador.

El callback `POST` no dependerá de que el navegador envíe la cookie `SameSite=Lax`; resuelve el intento por `state` y emite la cookie nueva en su respuesta.

### 12. Persistencia incremental sin una entidad nueva de sesión

Una migración posterior a V5 ampliará `identity_verification` con columnas descritas en español para modo de proveedor, hash y expiración/consumo de state, PKCE protegido, `session_state`, hash de subject, hash y vigencia/invalidez de autorización y actualización técnica. Se añadirán unicidad de `state_hash`, unicidad `(request_id, attempt_number)` e índices para resolver state e intento vigente.

No se añadirá `@Version`: la exclusión de callbacks se resolverá con una actualización condicional atómica y conteo de filas, que es más simple para esta transición puntual. No se guardarán `code`, access token, id token, JWT userinfo, client secret ni payloads.

### 13. Experiencia frontend sin simular la biometría

La ruta `/verificacion-identidad` usará el stepper vigente de cinco pasos y la composición de `step-1.png`. Informará que ID Perú verificará la identidad, que todavía no se cancelará ningún certificado y que después se pasará a selección. El botón pedirá la URL al backend una vez y ejecutará una navegación completa.

La ruta de retorno mostrará procesamiento mientras consulta `current`, y distinguirá éxito, cancelación, rechazo, expiración, identidad diferente e indisponibilidad. Reintentar creará un intento nuevo solo cuando el estado lo permita. Se evitarán doble envío, almacenamiento web de datos y mensajes basados únicamente en color. No se mostrará una pantalla ficticia del paso 2.

### 14. Seguridad web limitada al flujo actual

Se incorporará una cadena de seguridad o filtro pequeño para leer y validar la cookie en rutas protegidas. Inicio, callback, health y documentación local conservarán su acceso previsto; las futuras APIs posteriores a identidad deberán declarar protección explícita. CORS mantendrá orígenes exactos y las llamadas frontend usarán `credentials: include`. Operaciones autenticadas con cookie validarán origen permitido; `state` protege el callback.

No se añadirá login de usuario/contraseña, roles, refresh tokens ni un endpoint de emisión general de JWT.

### 15. Logout de ID Perú queda limitado por el contrato disponible

El cierre local siempre invalidará la autorización y la cookie. `logout_uri` se externalizará porque el documento la declara, pero no se enviarán parámetros ni se asumirá un retorno que la versión 1.2 no especifica. La invocación de logout remoto solo se habilitará cuando RENIEC confirme su contrato para este convenio.

## Risks / Trade-offs

- **[No hay credenciales ni convenio confirmados]** → completar y probar el adaptador contra un servidor controlado; marcar la validación institucional como verificación manual condicionada, sin sustituirla por credenciales ajenas.
- **[El PDF contiene una inconsistencia `idaas`/`idaas2` en `/userinfo`]** → usar siempre `userinfo_uri` institucional configurada, no codificar una URL del ejemplo, y solicitar confirmación antes de habilitar real.
- **[La versión 1.2 no detalla todos los claims del `id_token` ni logout]** → exigir los mínimos OIDC configurables y no inventar parámetros de logout; confirmar issuer y claims con RENIEC.
- **[Rotación de la clave interna PKCE durante intentos abiertos]** → mantener versión de clave o invalidar esos intentos de forma controlada antes de retirar la clave anterior.
- **[Callback duplicado o concurrente]** → consumo atómico del state antes del intercambio; el segundo callback recibe un resultado controlado y no reutiliza el código.
- **[Falla después de consumir state pero antes de terminar]** → registrar el intento como error/reintento requerido; nunca volver a usar código o verifier inciertos.
- **[JWKS no disponible o rota]** → caché TTL, una actualización por `kid` desconocido y error temporal sin aceptar tokens no verificables.
- **[SameSite y callback POST]** → no depender de la cookie de inicio durante el callback; resolver por state y emitir la autorización en la respuesta.
- **[Modo mock accidental en producción]** → validación de arranque que prohíba `mock` bajo perfil productivo.
- **[Una cookie robada dentro de su TTL]** → TLS, `HttpOnly`, `Secure`, vida corta, jti hasheado, validación de estado e invalidación; no usar fingerprinting invasivo.

## Migration Plan

1. Incorporar y verificar el PDF/README y documentar las decisiones tomadas de la referencia.
2. Añadir la migración incremental y adaptar entidad/repositorio con pruebas MySQL desde V1 y desde V5.
3. Implementar primitivas de state, PKCE, protección temporal, configuración y validación criptográfica con pruebas unitarias.
4. Implementar el puerto, el simulador y los casos de uso de inicio/callback/estado/logout.
5. Implementar el adaptador real contra un servidor ID Perú controlado y cubrir token, JWKS, userinfo, rotación y errores.
6. Exponer APIs, seguridad, cookies, errores y OpenAPI; regenerar tipos TypeScript.
7. Implementar la vista y retorno del paso 1, pruebas de componentes e integración completa con mock.
8. Ejecutar builds, tests, lint, typecheck y pruebas MySQL. Con credenciales autorizadas, realizar la comprobación institucional y registrar solo el resultado, nunca las credenciales.

Rollback: deshabilitar el acceso al paso 1 y el modo real, limpiar cookies y conservar las columnas nuevas vacías; Flyway no revertirá destructivamente la migración. Ningún token o dato ciudadano requiere migración inversa.

## Open Questions

- Confirmar con RENIEC el `client_id`, `client_secret`, redirect URI, `issuer`, host de `/userinfo`, `Referer` y `acr_values` autorizados para este proyecto y ambiente.
- Confirmar si el convenio exige `max_age` y qué valor corresponde.
- Confirmar los claims obligatorios adicionales del `id_token` y del JWT de `/userinfo`, especialmente `iss` cuando no figure en el ejemplo.
- Confirmar el contrato completo de `/logout`; hasta entonces solo se implementará invalidación local segura.
