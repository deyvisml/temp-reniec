## Why

La integración con ID Perú expone actualmente como variables de entorno numerosas decisiones técnicas internas, lo que dificulta comprender, ejecutar y desplegar el backend sin aportar flexibilidad útil al MVP. La configuración debe limitarse a los valores que realmente cambian entre ambientes o que son secretos, manteniendo los valores operativos estables dentro de la aplicación.

## What Changes

- Clasificar cada propiedad actual de ID Perú como secreta, dependiente del ambiente, decisión interna estable, valor exclusivo del mock u opción sin uso real.
- Mantener externalizados únicamente las credenciales, ubicaciones institucionales y un secreto maestro requerido por la protección temporal del flujo.
- Derivar las URI de callback y retorno desde URL base del backend y frontend, evitando variables específicas para rutas conocidas por la aplicación.
- Sustituir las dos claves técnicas de PKCE y continuidad por un único secreto maestro, derivando internamente claves independientes para cada propósito.
- Convertir timeouts, vigencias, nombre de cookie, seguridad de cookie, `acr_values` y caché JWKS en valores internos o definidos por perfil, sin variables de entorno para el uso normal.
- Eliminar propiedades sin uso vigente, como el endpoint de cierre de sesión, hasta que una funcionalidad real las necesite.
- Mantener el escenario mock como configuración exclusivamente local o de pruebas.
- Simplificar `IdPeruProperties`, los YAML, `.env.example`, documentación y pruebas sin reducir las validaciones OAuth/OIDC, PKCE, JWT, JWKS, cookies ni protección de secretos.
- **BREAKING (configuración):** retirar variables `ID_PERU_*` redundantes; los entornos existentes deberán adoptar el conjunto mínimo documentado.

## Capabilities

### New Capabilities

- `id-peru-configuration`: Define el contrato mínimo de configuración, sus valores internos y la separación entre configuración real, local y de pruebas.

### Modified Capabilities

Ninguna.

## Impact

- Configuración Spring Boot en `application.yml`, `application-local.yml`, `application-test.yml` y futuros perfiles productivos.
- Clase `IdPeruProperties` y componentes que consumen claves, URI, timeouts, vigencias y cookies.
- Archivos `.env.example`, documentación de ejecución e integración y pruebas de configuración.
- No cambia el contrato HTTP del frontend, los endpoints existentes, el modelo de datos ni el flujo funcional con ID Perú.
