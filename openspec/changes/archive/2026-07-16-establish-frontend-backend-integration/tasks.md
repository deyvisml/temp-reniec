## 1. API técnica versionada en backend

- [x] 1.1 Añadir `springdoc-openapi-starter-webmvc-api` 3.0.3 al Maven backend y comprobar que no incorpora Swagger UI ni otra dependencia preventiva directa.
- [x] 1.2 Crear el DTO de estado y un servicio técnico que ejecute `SELECT 1` mediante el datasource existente sin consultar tablas de dominio.
- [x] 1.3 Exponer únicamente `GET /api/v1/system/status` con respuesta `status`, `database` y `timestamp` saneada.
- [x] 1.4 Incorporar la excepción de dependencia no disponible y mapearla a HTTP 503 mediante el `ApiError` común y la correlación existente.
- [x] 1.5 Configurar CORS para `/api/**` mediante una lista exacta de orígenes, métodos y headers, habilitando credenciales y exponiendo `X-Correlation-ID` sin comodines.
- [x] 1.6 Añadir variables y valores de perfil para origen local, habilitación de OpenAPI y escaneo exclusivo de `/api/v1/**`, manteniendo producción diferida.
- [x] 1.7 Definir metadatos y anotaciones OpenAPI suficientes para documentar éxito, error 503, DTOs y header de correlación sin exponer Actuator ni rutas de prueba.

## 2. Verificación contractual del backend

- [x] 2.1 Añadir pruebas rápidas del servicio/controlador y del error 503 que comprueben mensajes públicos y ausencia de detalles JDBC o SQL.
- [x] 2.2 Añadir pruebas con MySQL Testcontainers para confirmar que `/api/v1/system/status` ejecuta la consulta real, responde `UP` y propaga correlación.
- [x] 2.3 Probar preflight CORS permitido desde `http://localhost:3000`, rechazo de un origen no configurado y exposición de `X-Correlation-ID`.
- [x] 2.4 Probar `/v3/api-docs` y verificar que contiene solo el contrato `/api/v1/**`, schemas y respuestas esperadas, sin Actuator ni endpoints de prueba.

## 3. Contrato y transporte del frontend

- [x] 3.1 Añadir `NEXT_PUBLIC_BACKEND_URL` al ejemplo y resolver de forma explícita URL servidor/navegador sin exponer secretos.
- [x] 3.2 Añadir `openapi-typescript` como dependencia de desarrollo y scripts `api:sync` y `api:check` mediante una utilidad Node pequeña y multiplataforma.
- [x] 3.3 Generar y versionar la copia canónica `openapi/backend-api.json` y `lib/api/generated.ts` desde el OpenAPI real del backend.
- [x] 3.4 Crear aliases mínimos de contrato para estado y error derivados de los tipos generados, sin duplicar DTOs manualmente ni crear un SDK.
- [x] 3.5 Ampliar `requestJson` con correlación saliente, URL pública/servidor, timeout de 8 segundos, cancelación externa, éxito vacío y clasificación segura de todos los fallos previstos.
- [x] 3.6 Crear una función específica y tipada `getSystemStatus` que use el cliente central y el path generado `/api/v1/system/status`.

## 4. Comprobación visible temporal

- [x] 4.1 Crear un componente cliente de estado de integración con consulta inicial única, estados textuales accesibles y reintento manual.
- [x] 4.2 Integrar el componente en la página temporal, reemplazando el texto de integración pendiente sin añadir controles del flujo ciudadano.
- [x] 4.3 Garantizar que la página sigue siendo utilizable sin backend, no realiza polling y solo muestra mensajes públicos y correlación opcional.

## 5. Pruebas frontend e integración real

- [x] 5.1 Ampliar pruebas unitarias del cliente para URL, correlación, timeout, cancelación, errores HTTP/red, JSON inválido y respuesta vacía usando fetch controlado.
- [x] 5.2 Probar la función de estado y las vistas puras del indicador para estados comprobando/disponible/no disponible sin añadir jsdom ni herramientas E2E.
- [x] 5.3 Añadir pruebas que compilen los aliases generados y verificar que `api:check` detecta una copia o tipos desalineados.
- [x] 5.4 Crear una suite `test:integration` separada que use el cliente y los tipos reales contra el backend local y valide `UP`, MySQL y correlación sin mocks.

## 6. Documentación y comprobación integral

- [x] 6.1 Actualizar el README backend con API v1, estado, CORS, OpenAPI, variables y comandos de pruebas, sin documentar contratos funcionales inexistentes.
- [x] 6.2 Actualizar el README frontend con variables servidor/públicas, generación y control de tipos, indicador temporal y suite real separada.
- [x] 6.3 Documentar el recorrido completo MySQL `3307` → backend `8080` → frontend `3000`, incluyendo inicio, health, sincronización, pruebas y apagado.
- [x] 6.4 Ejecutar `mvn clean verify` y confirmar pruebas rápidas, persistencia e integración backend con MySQL desechable.
- [x] 6.5 Ejecutar instalación reproducible, generación/check de contrato, typecheck, pruebas unitarias y build del frontend.
- [x] 6.6 Levantar el stack local, ejecutar la suite frontend real, comprobar visualmente/por HTTP el indicador y confirmar correlación y MySQL disponibles.
- [x] 6.7 Detener procesos temporales conservando el volumen, retirar archivos privados o logs de prueba y validar estrictamente el cambio OpenSpec.
