# Integración con ID Perú

## Fuente técnica vigente

El archivo [`IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf`](./IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf) es la referencia técnica principal para la integración con la Plataforma Nacional de Autenticación de la Identidad Digital ID Perú.

- Versión: **1.2**.
- Estado: **Aprobado**.
- Fecha del documento: **22 de mayo de 2026**.
- Páginas: **22**.
- Tamaño: **699425 bytes**.
- SHA-256: `56AEE54D80A3743628B046FBA1ED8A35107E64F2AFD51CC5587CC0DCF47308FB`.

Toda tarea posterior relacionada con autenticación, OAuth 2.0, OpenID Connect, PKCE, `state`, `vd`, tokens, JWKS, datos del ciudadano, callback, cierre de sesión o seguridad de ID Perú debe revisar primero este documento y el contexto funcional vigente.

## Partes de la implementación dependientes

El documento gobierna la construcción de `/auth`, `acr_values`, el cifrado de `vd`, PKCE `S256`, el callback, `/token`, `/userinfo`, `/jwks` y el tratamiento temporal de códigos y tokens. El contexto del proyecto prevalece para las reglas del trámite: autenticarse solo habilita la selección; no cancela credenciales ni recupera solicitudes anteriores.

## Configuración institucional pendiente

Los valores que deben obtenerse mediante convenio y canales autorizados son `client_id`, `client_secret`, la URI base efectiva de ID Perú v2, el Referer permitido y las URLs públicas registradas para frontend y backend. El redirect URI resultante debe registrarse en ID Perú. El secreto maestro local del flujo se genera y custodia dentro de la infraestructura del proyecto; no lo entrega el proveedor.

El PDF describe ID Perú v2. Las credenciales locales actualmente autorizadas pertenecen al contrato v1 y utilizan `idaas.reniec.gob.pe`; una credencial v2 utiliza `idaas2.reniec.gob.pe`. La versión configurada debe coincidir con el convenio y las credenciales recibidas.

No se incorporan credenciales, secretos, tokens ni archivos `.env` en esta documentación. Las credenciales del proyecto de referencia no están autorizadas para este sistema.

## Decisiones tomadas del proyecto de referencia

Se revisó en modo lectura `C:\FastFolder\sistema-autorizacion-credenciales-reniec`.

Se reutilizan, adaptadas, la separación de proveedor real/simulado, propiedades tipadas, timeouts, `Referer`, hash de `state`, PKCE, cifrado de `vd`, JWKS y retorno controlado. Se descartan circuit breaker, reintentos generales, reconstrucción determinista del verifier y el modelo de sesiones de aquel dominio.

Aquí el verifier es aleatorio, se protege temporalmente y se elimina al finalizar. El inicio exige la sesión transaccional activa y una verificación exitosa eleva esa misma sesión. `identity_verification` conserva el intento, no tokens ni una autorización paralela. También conserva el claim obligatorio `first_name`, que el contrato define como el primer nombre del ciudadano, exclusivamente en intentos `VERIFIED`. El dato se normaliza y se utiliza en la revisión autenticada del paso 4, el resultado autenticado del paso 5 y las nuevas constancias PDF; no se incluye en endpoints previos a la autenticación, logs, errores, auditoría o referencias técnicas.

El callback OAuth/OIDC permanece en el backend y usa uniformemente `/api/v1/idperu/callback`. En local resulta en `http://localhost:8080/api/v1/idperu/callback`; en producción se combina con la base HTTPS productiva. En local, el paso 1 se presenta y el callback retorna a `http://localhost:3000/autorizacion`; no se redirige después a `/revocacion`. Producción presenta el paso y retorna directamente a `/revocacion`. El resultado se obtiene desde el contexto temporal validado por el backend; no se incluyen códigos, tokens, DNI, identificadores ni estados en la URL.

### Transporte y redirección del callback

La ruta de callback admite `GET` con parámetros de consulta —transporte utilizado por las credenciales locales v1— y mantiene `POST application/x-www-form-urlencoded` para compatibilidad con el proveedor. Ambos transportes delegan en el mismo caso de uso, por lo que conservan las mismas validaciones de `state`, vigencia, uso único y campos requeridos por versión.

El navegador nunca queda mostrando la respuesta técnica del endpoint. Después de procesar un éxito, revocación, rechazo o error controlado, el backend responde `303 See Other` hacia la URI frontend fija del ambiente. La respuesta no incorpora `code`, `state`, `session_state`, tokens, DNI ni diagnósticos del proveedor en `Location` o en el cuerpo. Un éxito emite únicamente la autorización temporal; un fallo conserva el paso 1 y entrega un resultado efímero normalizado para mostrar un solo aviso ciudadano.

Esta regla corrige la restricción anterior que documentaba el callback únicamente como `POST`: ID Perú v1 retorna mediante `GET`, mientras que v2 y futuras configuraciones pueden utilizar el transporte registrado sin crear controladores o flujos paralelos.

## Contrato mínimo de configuración

El perfil local usa por defecto la integración simulada y permite seleccionar ID Perú v1 real mediante `ID_PERU_MODE=real`; sus credenciales se obtienen del archivo privado `backend/.env`. Producción usa obligatoriamente ID Perú v2 real y recibe sus secretos desde la plataforma de despliegue. La validación de arranque impide activar el simulador bajo el perfil `prod`.

En el escenario local exitoso, el simulador devuelve el mismo DNI válido de 8 dígitos que inició la solicitud y el primer nombre sintético `PRUEBA`. Así se conserva la comparación de identidad del caso de uso sin asociar el entorno de pruebas a un DNI particular.

Para modo real se requieren las siguientes bases y credenciales. Local aporta el referer registrado como valor predeterminado; producción debe suministrarlo externamente:

- `APP_FRONTEND_BASE_URL`
- `APP_BACKEND_BASE_URL`
- `ID_PERU_CLIENT_ID`
- `ID_PERU_CLIENT_SECRET`
- `ID_PERU_REFERER`
- `ID_PERU_FLOW_SECRET`

El perfil activo selecciona un contrato completo y evita mezclar endpoints o parámetros: `local` usa v1, deriva los endpoints desde `https://idaas.reniec.gob.pe/`, utiliza `scope=openid profile` y no envía PKCE; `prod` usa v2, deriva los endpoints desde `https://idaas2.reniec.gob.pe/`, utiliza `scope=openid` y PKCE S256. El secreto maestro debe ser Base64 de 32 bytes; mediante HMAC-SHA-256 se derivan claves independientes para los artefactos temporales del flujo y la continuidad. Una rotación invalida de forma controlada los flujos temporales activos.

La URL de autorización v1 se construye exclusivamente con los parámetros documentados. No se agrega el fragmento `#!/auth-qr-face`, porque no forma parte de la especificación técnica v1; el mecanismo facial se solicita mediante `acr_values=face_mobile`.

Timeouts, vigencias, caché JWKS, ACR `face_mobile`, nombre de cookie y seguridad por perfil son decisiones internas del MVP. Logout remoto y `max_age` no se configuran porque no existe actualmente un caso de uso que los consuma.

### Comprobación local real

1. Copiar `backend/.env.example` como `backend/.env`.
2. Configurar `ID_PERU_MODE=real`.
3. Completar `ID_PERU_CLIENT_ID` e `ID_PERU_CLIENT_SECRET` con valores autorizados. El perfil local ya utiliza v1 y el referer `http://localhost:3000/autorizacion`; producción utiliza v2. `ID_PERU_REFERER` solo debe declararse localmente si el registro autorizado fuese distinto.
4. Confirmar que el cliente de prueba tenga registrados exactamente el callback `http://localhost:8080/api/v1/idperu/callback` y el origen/retorno `http://localhost:3000/autorizacion`.
5. Levantar MySQL y el backend con el perfil `local`, iniciar el frontend y completar la consulta con el mismo DNI que autenticará ID Perú.
6. Verificar la redirección institucional, el callback de un solo uso y que el paso 1 y su resultado permanezcan en `/autorizacion` durante la ejecución local.

Para volver al simulador cambia a `ID_PERU_MODE=mock`. Con el stack Docker iniciado, aplica cualquiera de los dos modos mediante `docker compose restart backend`; el archivo está montado en el contenedor y no requiere reconstruir la imagen.

Un error de credenciales, callback, `Referer`, firma, audiencia, issuer o correspondencia de DNI detiene el flujo. Nunca debe resolverse cambiando código, desactivando PKCE/JWKS o eliminando la comparación de identidad.

La solicitud de `/service/auth` codifica cada valor de consulta exactamente una vez. Al diagnosticar la URL, `redirect_uri` debe aparecer en la consulta cruda como `http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fidperu%2Fcallback`, no como una URL anidada sin codificar. Si RENIEC sigue mostrando "Actividad no autorizada" con esa forma, se debe conservar el número de caso y confirmar con la mesa de ayuda la habilitación del `client_id`, callback, mecanismo `face_mobile` y red de origen.

El `state` contiene 256 bits aleatorios y se representa con Base64 URL-safe sin relleno. Esta adaptación conserva la entropía requerida y evita que el carácter `+` de Base64 convencional se convierta en espacio al atravesar query strings y formularios `application/x-www-form-urlencoded`.

## Estado de comprobación

La implementación se valida automáticamente contra un servidor controlado y con un simulador. Cuando existan credenciales autorizadas, el mismo perfil local permite la comprobación institucional cambiando solo valores de `.env`; las pruebas simuladas no equivalen a certificación productiva.
