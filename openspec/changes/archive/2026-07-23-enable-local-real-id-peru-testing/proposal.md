## Why

La integración real de ID Perú quedó acoplada al perfil `prod`, mientras el perfil `local` fuerza el simulador. Esto impide reproducir desde desarrollo el flujo institucional con credenciales de prueba, contradice la estrategia del proyecto de referencia y deja el incremento sin una comprobación manual completa.

Además, el mock inicial de disponibilidad solo permite continuar con un DNI ficticio fijo. Para verificar una identidad real o institucional de prueba debe poder confirmarse disponibilidad para el DNI elegido localmente, sin modificar código ni debilitar los escenarios deterministas de error.

## What Changes

- Desacoplar el perfil de Spring del modo de ID Perú: `local` admitirá `mock` o `real` mediante `ID_PERU_MODE`, conservando `mock` como valor predeterminado.
- Mantener una garantía de arranque que obligue a producción a utilizar el adaptador real.
- Permitir configurar desde `backend/.env` solamente las credenciales de prueba necesarias para ID Perú real local, con la raíz institucional fija y el referer local registrado como valor predeterminado.
- Admitir HTTP exclusivamente para URLs de `localhost` en desarrollo, incluido el referer registrado `http://localhost:3000/autorizacion`; la URL del proveedor y toda configuración productiva continuarán exigiendo HTTPS.
- Usar uniformemente el callback `/api/v1/idperu/callback`; en local corresponde a `http://localhost:8080/api/v1/idperu/callback` y en producción se combina con la base HTTPS productiva. El retorno frontend local será `http://localhost:3000/autorizacion` y el productivo continuará en `/cancelacion`.
- Hacer que cualquier DNI válido no reservado para escenarios especiales obtenga el resultado exitoso en el mock local de disponibilidad, manteniendo fixtures ficticios deterministas para resultados negativos, no concluyentes y fallos.
- Evitar almacenar o registrar el DNI local configurable, credenciales, tokens o secretos fuera de la configuración ignorada por Git.
- Documentar dos comandos locales claros: flujo simulado y flujo real con credenciales de prueba.
- Agregar pruebas de selección de adaptadores, validación de configuración y disponibilidad positiva configurable.

## Capabilities

### New Capabilities

- `id-peru-runtime-mode`: Selección segura e independiente del adaptador mock o real de ID Perú en desarrollo, con producción obligatoriamente real y configuración externa validada.

### Modified Capabilities

- `citizen-eligibility-entry`: El mock local conserva fixtures deterministas para resultados alternativos y usa el resultado positivo como comportamiento normal de cualquier DNI válido no reservado, permitiendo pruebas integradas con ID Perú.

## Impact

- Configuración de Spring Boot en `application-local.yml`, `application-prod.yml` y propiedades de ID Perú.
- Adaptadores condicionales de identidad y mock local de disponibilidad.
- `backend/.env.example`, documentación operativa de backend e integración ID Perú.
- Pruebas unitarias y de contexto de configuración.
- Presenta el paso 1 en `/autorizacion` durante la ejecución local reutilizando el componente del flujo, mientras producción conserva `/cancelacion`; no cambia tablas ni el protocolo OAuth/OIDC implementado.
