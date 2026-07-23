## Context

La configuración vigente de ID Perú declara 25 propiedades bajo `app.id-peru`, casi todas respaldadas por una variable de entorno. Aunque los valores se consumen, no todos constituyen configuración operativa: las rutas del protocolo, las vigencias, los timeouts, el nombre de cookie y varios valores predeterminados son decisiones internas estables del MVP.

El proyecto de referencia también externaliza credenciales y endpoints, pero su integración soporta simultáneamente ID Perú v1, v2, circuit breaker y rutas de éxito/error separadas. Este proyecto solo implementa ID Perú v2, una ruta frontend canónica y un flujo temporal acotado, por lo que copiar esa amplitud no está justificado.

## Goals / Non-Goals

**Goals:**

- Reducir de forma visible el bloque YAML y el número de variables necesarias para ejecutar o desplegar ID Perú.
- Mantener externalizados únicamente secretos y valores que realmente cambian por ambiente.
- Conservar validaciones criptográficas, PKCE, `state`, JWKS, correspondencia de DNI y cookies seguras.
- Permitir desarrollo local en modo mock sin completar manualmente variables.
- Fallar al iniciar cuando el modo real no tenga su configuración mínima.

**Non-Goals:**

- Cambiar endpoints HTTP, DTO, estados funcionales o comportamiento visible del flujo.
- Modificar OAuth 2.0, OpenID Connect, PKCE o el contrato institucional.
- Añadir descubrimiento OIDC, circuit breaker, logout o nuevos mecanismos de autenticación.
- Preparar configuraciones hipotéticas para proveedores o versiones no confirmadas.

## Decisions

### 1. Contrato externo mínimo y explícito

En modo real se utilizarán únicamente:

- `APP_FRONTEND_BASE_URL`
- `APP_BACKEND_BASE_URL`
- `ID_PERU_BASE_URI`
- `ID_PERU_CLIENT_ID`
- `ID_PERU_CLIENT_SECRET`
- `ID_PERU_REFERER`
- `ID_PERU_FLOW_SECRET`

El modo se definirá por perfil (`local=mock`, producción futura=`real`, base=`disabled`) y no requerirá normalmente `ID_PERU_MODE`. El escenario mock permanecerá como opción exclusiva del perfil local/pruebas.

Alternativa descartada: conservar cada propiedad como variable por si cambia. Esa flexibilidad hipotética es precisamente la fuente de complejidad y no coincide con el alcance v2 confirmado.

### 2. URI derivadas a partir de bases conocidas

`ID_PERU_BASE_URI` representará la raíz institucional v2. El adaptador derivará las rutas confirmadas de autorización, token, userinfo y JWKS. El issuer se validará contra la base institucional normalizada.

El callback se derivará de `APP_BACKEND_BASE_URL` y la ruta backend fija. El retorno se derivará de `APP_FRONTEND_BASE_URL` y `/cancelacion`. La aplicación, no el operador, es propietaria de esas rutas.

Alternativa descartada: mantener seis URI independientes. El proyecto de referencia las necesita por soportar más variantes; este sistema no.

### 3. Valores técnicos estables como constantes internas

Se mantendrán en una única ubicación del backend los valores actuales:

- conexión: 3 segundos;
- lectura: 5 segundos;
- vigencia de `state`: 5 minutos;
- autorización previa a ID Perú: 10 minutos;
- autorización posterior: 15 minutos;
- caché JWKS: 15 minutos;
- nombre de cookie: `cancelacion_flow`;
- mecanismo inicial: `face_mobile`.

`max_age` y `logout_uri` se eliminarán mientras no exista un caso de uso vigente. `cookieSecure` tendrá valor seguro por defecto y solo se desactivará explícitamente en el perfil local, sin variable de entorno.

Alternativa descartada: mantener ajustes operativos para cada valor. En el MVP no existe evidencia de que deban variar independientemente entre despliegues.

### 4. Un secreto maestro con separación criptográfica interna

`ID_PERU_FLOW_SECRET` será un valor Base64 de 32 bytes. A partir de él se derivarán dos claves diferentes mediante HMAC-SHA-256 y etiquetas de contexto distintas: una para AES-GCM del verificador PKCE temporal y otra para firmar la autorización de continuidad.

No se reutilizará directamente la misma clave para ambos algoritmos. La derivación evita dos variables externas sin debilitar la separación entre propósitos y no requiere una dependencia adicional.

Alternativas descartadas: conservar dos secretos externos, por carga operativa; o reutilizar una misma clave sin derivación, por mala práctica criptográfica.

### 5. Propiedades pequeñas y agrupadas por responsabilidad

`IdPeruProperties` contendrá únicamente el contrato externo mínimo y helpers derivados. Los valores constantes del flujo se ubicarán en una configuración interna inmutable. El mock no necesitará credenciales ni endpoints reales.

Las pruebas comprobarán tanto el conjunto permitido de variables como la ausencia de los nombres retirados en YAML, documentación y ejemplos.

## Risks / Trade-offs

- **[Una ruta institucional cambia sin cambiar la base]** → El cambio requerirá una modificación localizada y probada del adaptador; no se reintroducirán overrides preventivos.
- **[Migración de entornos existentes]** → Documentar equivalencias y rechazo explícito de variables antiguas; al no existir despliegue productivo consolidado, la migración se realizará antes de producción.
- **[Un secreto maestro concentra la rotación]** → Derivar claves con contextos diferentes y exigir 32 bytes aleatorios; una rotación invalida de forma controlada los flujos temporales activos.
- **[Valores internos requieren despliegue para ajustarse]** → Aceptado para el MVP; solo se externalizará un valor cuando exista una necesidad operativa comprobada.

## Migration Plan

1. Introducir URLs base y el secreto maestro con derivación de claves.
2. Adaptar consumidores y pruebas conservando el comportamiento funcional.
3. Derivar las URI y retirar campos obsoletos de `IdPeruProperties`.
4. Simplificar los YAML y archivos de ejemplo.
5. Actualizar la documentación con el conjunto mínimo y una tabla de migración.
6. Ejecutar pruebas unitarias, integración ID Perú, arranque local y compilación.

Rollback: restaurar las propiedades anteriores y sus consumidores; no existe migración de datos ni cambio contractual que revertir.

## Open Questions

Ninguna para el MVP. Si RENIEC entrega endpoints que no compartan la base institucional prevista, esa evidencia habilitará una configuración específica posterior.
