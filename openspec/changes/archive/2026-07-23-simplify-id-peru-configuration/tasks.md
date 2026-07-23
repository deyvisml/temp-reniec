## 1. Contrato de configuración

- [x] 1.1 Crear pruebas que definan el conjunto mínimo permitido de propiedades y detecten variables `ID_PERU_*` retiradas
- [x] 1.2 Incorporar URLs base de frontend/backend y validar su formato según el ambiente
- [x] 1.3 Reducir `IdPeruProperties` a modo, base institucional, credenciales, referer, secreto maestro y escenario mock
- [x] 1.4 Centralizar timeouts, vigencias, caché, cookie y `acr_values` como valores internos inmutables

## 2. Derivación segura y rutas

- [x] 2.1 Implementar la derivación de claves independientes para PKCE y firma desde `ID_PERU_FLOW_SECRET`
- [x] 2.2 Adaptar el protector PKCE y el token temporal para consumir las claves derivadas
- [x] 2.3 Derivar autorización, token, userinfo, JWKS e issuer desde `ID_PERU_BASE_URI`
- [x] 2.4 Derivar callback y retorno desde las URLs base y las rutas canónicas del backend/frontend
- [x] 2.5 Eliminar `logout-uri`, `max-age` y consumidores o validaciones sin caso de uso vigente

## 3. Perfiles y documentación

- [x] 3.1 Simplificar `application.yml`, `application-local.yml` y `application-test.yml` sin valores redundantes
- [x] 3.2 Configurar modo y seguridad de cookie por perfil, manteniendo desarrollo local sin credenciales reales
- [x] 3.3 Actualizar `.env.example`, README del backend y documentación de ID Perú con el conjunto mínimo y la migración de variables
- [x] 3.4 Confirmar que ningún secreto, credencial real o variable retirada permanezca en ejemplos, logs o respuestas

## 4. Verificación

- [x] 4.1 Probar validación de configuración mínima, URI derivadas y rechazo de valores inseguros o ausentes
- [x] 4.2 Probar que las claves derivadas son deterministas, diferentes por propósito y no exponen el secreto maestro
- [x] 4.3 Ejecutar pruebas del adaptador real, mock, PKCE, callback, JWT/JWKS, cookies y correspondencia de DNI
- [x] 4.4 Verificar arranque local, compilación y pruebas completas del backend
- [x] 4.5 Confirmar que los endpoints y contratos OpenAPI no cambiaron
