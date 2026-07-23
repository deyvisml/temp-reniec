## Context

El backend ya contiene adaptadores intercambiables `mock`, `real` y `disabled` para ID Perú, además del flujo OAuth/OIDC v2 con `state`, PKCE, intercambio de tokens, JWKS y `/userinfo`. Sin embargo, `application-local.yml` fija `mode: mock` y `application-prod.yml` fija `mode: real`; la documentación incluso indica que el modo no se selecciona. El proyecto de referencia separa el ambiente de ejecución del proveedor elegido y permite desarrollo real con credenciales externas.

El mock local de disponibilidad también reserva un único DNI ficticio para `AVAILABLE` y devuelve `INCONCLUSIVE` para cualquier otro. Esto impide que una persona autorizada pruebe la correspondencia entre el DNI ingresado y la identidad retornada por ID Perú real.

## Goals / Non-Goals

**Goals:**

- Permitir `ID_PERU_MODE=mock` y `ID_PERU_MODE=real` bajo el perfil `local`.
- Mantener el modo local simulado como predeterminado y producción obligatoriamente real.
- Hacer que la prueba real local solo requiera completar credenciales y datos institucionales en `backend/.env`.
- Permitir que un DNI válido normal avance en la disponibilidad local, reservando valores ficticios documentados para escenarios alternativos.
- Conservar secretos fuera de Git y validar temprano configuraciones incompletas.

**Non-Goals:**

- Integrar el servicio institucional real de disponibilidad de certificados.
- Incorporar credenciales de ID Perú al repositorio.
- Cambiar el protocolo OAuth/OIDC, la persistencia o la interfaz del flujo ciudadano.
- Relajar los controles de firma, `state`, PKCE, correspondencia de DNI o HTTPS del proveedor.

## Decisions

### 1. Separar perfil y modo mediante una propiedad local

`application-local.yml` usará `app.id-peru.mode: ${ID_PERU_MODE:mock}`. Los beans existentes condicionados por propiedad seleccionarán el adaptador sin introducir nuevos perfiles combinatorios. `application-prod.yml` conservará `mode: real` y la validación impedirá ejecutar producción con otro modo.

Se descarta crear perfiles `local-real` y `local-mock` porque duplicarían configuración de base de datos, CORS, Swagger y reCAPTCHA.

### 2. Configurar ID Perú real local desde el `.env` existente

El perfil local importará opcionalmente `backend/.env` como ya ocurre hoy. Se declararán `ID_PERU_CLIENT_ID` e `ID_PERU_CLIENT_SECRET`, exigidos solamente cuando `ID_PERU_MODE=real`; `ID_PERU_REFERER` quedará disponible solo como sobrescritura excepcional del valor local registrado. La raíz institucional `https://idaas2.reniec.gob.pe/` permanecerá en la configuración versionada porque no es secreta ni varía entre los ambientes confirmados. El secreto de flujo local mantendrá un valor exclusivamente de desarrollo para evitar trabajo manual ajeno a las credenciales del proveedor; producción seguirá requiriendo un secreto externo.

Las URLs `http://localhost` de frontend y backend se aceptarán solo con perfiles `local` o `test`. El referer también podrá ser HTTP exclusivamente en `localhost`, porque las credenciales de prueba fueron emitidas para `http://localhost:3000/autorizacion`. La raíz institucional siempre requerirá HTTPS y producción no aceptará ningún origen HTTP.

### 3. Resultado exitoso como camino normal del mock de disponibilidad

Los DNI ficticios `00000002` a `00000006` conservarán los escenarios negativo, indisponible, no concluyente, error y timeout. Cualquier otro DNI válido, incluido `00000001`, devolverá `AVAILABLE`. El adaptador continuará limitado a `local` y `test`, por lo que este comportamiento no puede activarse en producción.

Se descarta configurar un único DNI exitoso porque obligaría a editar `.env` para cada identidad de prueba y repetiría la limitación actual.

### 4. Documentar una receta verificable

La documentación separará claramente:

- Local simulado: `ID_PERU_MODE=mock`, sin credenciales.
- Local real: `ID_PERU_MODE=real`, `client_id` y `client_secret` autorizados.

La verificación manual exigirá que el callback local esté autorizado para las credenciales de prueba. Si el proveedor no admite `localhost`, se usarán las URLs HTTPS del túnel autorizado en las propiedades de base de aplicación, sin modificar código.

### 5. Uniformizar el callback y presentar el paso 1 en la ruta registrada por ambiente

Todos los ambientes anunciarán y atenderán `/api/v1/idperu/callback`, combinado con la base del backend correspondiente. En local, el resultado positivo de la consulta inicial navegará a `/autorizacion`, donde se presentará realmente el paso 1 y donde retornará ID Perú, exactamente como fueron registradas las credenciales de prueba. La ruta reutilizará el mismo componente del flujo y resolverá su estado desde el backend; no transportará DNI, identificadores, códigos ni tokens. Fuera de local, `/autorizacion` redirigirá a `/cancelacion`, y producción presentará y retornará al paso 1 directamente en `/cancelacion`.

### 6. Codificar estrictamente los valores de la solicitud de autorización

La URL de `/service/auth` se construirá con variables de URI y expansión codificada, siguiendo el patrón comprobado del proyecto de referencia. De este modo, valores completos como `redirect_uri` y el Base64 de `vd` se codifican como datos del parámetro y no se interpretan como estructura de la URL exterior. No se pre-codificarán manualmente los valores, evitando dobles codificaciones.

## Risks / Trade-offs

- [Un DNI real obtiene disponibilidad positiva en local] → El mock solo existe en perfiles `local` y `test`, no consulta datos externos ni genera certificados.
- [Credenciales incompletas impiden iniciar] → La validación falla temprano con el nombre de la propiedad faltante.
- [El callback `localhost` no está autorizado por ID Perú] → La documentación exige registrar el callback o utilizar un túnel HTTPS autorizado.
- [Un desarrollador configura `real` accidentalmente] → El valor predeterminado local sigue siendo `mock` y nunca se incluyen credenciales de ejemplo funcionales.
