## 1. Referencias y línea base

- [x] 1.1 Copiar sin modificar el PDF adjunto a `docs/integrations/id-peru/IDAAS-V2-Especificaciones-Tecnicas-v1.2.pdf` y verificar tamaño, SHA-256, apertura y 22 páginas.
- [x] 1.2 Crear `docs/integrations/id-peru/README.md` con versión 1.2, fecha 22/05/2026, estado aprobado, áreas dependientes, configuración institucional pendiente y reglas de credenciales.
- [x] 1.3 Documentar decisiones reutilizadas, adaptadas y descartadas del proyecto `sistema-autorizacion-certificados-reniec`, sin copiar secretos ni configuración productiva.
- [x] 1.4 Actualizar las referencias vigentes de arquitectura, seguridad y ejecución para exigir consulta del PDF en futuros cambios ID Perú.

## 2. Migración y persistencia de identidad

- [x] 2.1 Crear una migración Flyway incremental posterior a V5 que amplíe `identity_verification` con modo, state hasheado, expiración/consumo, PKCE protegido, session_state, subject seguro y autorización temporal.
- [x] 2.2 Añadir comentarios de tabla y columnas en español, unicidad de state y `(request_id, attempt_number)`, claves e índices alineados con las consultas reales.
- [x] 2.3 Adaptar `IdentityVerificationEntity`, estados y repositorio sin almacenar tokens, códigos, DNI autenticado ni payloads completos.
- [x] 2.4 Implementar la reserva/consumo atómico del intento por state hash y la limpieza del PKCE protegido en todo resultado terminal.
- [x] 2.5 Implementar emisión e invalidación persistente del hash de jti sobre el intento verificado, sin tabla de sesiones ni refresh token.
- [x] 2.6 Añadir pruebas MySQL/Testcontainers para migración desde base vacía y V5, restricciones, callbacks concurrentes, replay e invalidación.

## 3. Configuración y primitivas de seguridad

- [x] 3.1 Añadir propiedades tipadas para modo, credenciales, URLs, issuer, redirect/return, Referer, acr, max_age, timeouts, TTLs, algoritmos y claves internas.
- [x] 3.2 Configurar mock local/test, real opt-in local y real obligatorio en producción con validación fail-closed y endpoints HTTPS.
- [x] 3.3 Actualizar `.env.example` y documentación con placeholders, manteniendo secretos fuera de Git y del frontend.
- [x] 3.4 Incorporar la dependencia JOSE/JWT mínima compatible con Spring Boot y verificar que no añada un servidor OAuth ni librerías JWT duplicadas.
- [x] 3.5 Implementar generación/hash de state aleatorio, expiración y comparación segura con pruebas de unicidad y formato.
- [x] 3.6 Implementar PKCE aleatorio S256, cifrado temporal AES-GCM del verifier con clave externa y eliminación al terminar, con vectores y pruebas de rotación/error.
- [x] 3.7 Implementar el cifrado `vd` AES/CBC/PKCS5Padding conforme al PDF, inclusión condicional por `acr_values` y pruebas con vectores ficticios.
- [x] 3.8 Implementar firma y validación de credenciales `IDENTITY_INIT` y `FLOW_AUTH` sin PII, con propósito, audiencia, jti y TTL separados.

## 4. Caso de uso y adaptador simulado

- [x] 4.1 Definir el puerto de proveedor de identidad y DTO internos sin tipos HTTP ni contratos específicos filtrados hacia el dominio.
- [x] 4.2 Implementar el caso de uso de inicio que resuelve la solicitud desde continuidad, valida disponibilidad/estado, crea el intento y construye la URL en backend.
- [x] 4.3 Implementar el caso de uso de callback con validación/consumo de state, manejo de error, token/userinfo, coincidencia y transiciones transaccionales.
- [x] 4.4 Implementar consulta del estado actual e invalidación local de la autorización/cookie.
- [x] 4.5 Implementar el adaptador ID Perú simulado y sus rutas solo local/test con escenarios deterministas de éxito, mismatch, rechazo, cancelación, expiración, timeout, indisponibilidad, respuesta inválida y replay.
- [x] 4.6 Probar que el mock atraviesa las mismas reglas de state, PKCE, callback, match, persistencia y autorización que el modo real.

## 5. Adaptador real ID Perú v2

- [x] 5.1 Implementar el constructor de `/auth` con parámetros v1.2, codificación única, acr configurable, max_age opcional y URL no registrable.
- [x] 5.2 Implementar el cliente `/token` form-urlencoded con Referer, PKCE, timeout, validación defensiva y prohibición de reintento automático.
- [x] 5.3 Implementar validación criptográfica completa del `id_token` con RS256, kid, firma, issuer, audience y vigencia.
- [x] 5.4 Implementar `/userinfo` POST Bearer/Referer y validar su JWT antes de extraer `sub` y `doc`.
- [x] 5.5 Implementar caché JWKS con TTL y un refresh por kid desconocido, contemplando rotación, caída y respuesta malformada.
- [x] 5.6 Implementar controles de consistencia de subject, coincidencia de DNI y normalización de errores sin exponer datos personales.
- [x] 5.7 Probar el adaptador real con un servidor HTTP controlado para éxito, token inválido/reutilizado, firma, claims, rotación JWKS, timeout e indisponibilidad.

## 6. API, cookies y seguridad web

- [x] 6.1 Emitir la continuidad HttpOnly al finalizar positivamente la consulta inicial y ajustar la navegación para no incluir requestId ni DNI en la URL.
- [x] 6.2 Implementar `POST /api/v1/identity-verifications` y `GET /api/v1/identity-verifications/current` con correlación y errores comunes.
- [x] 6.3 Implementar el callback POST form, consumo único y redirección 303 fija sin artefactos sensibles en URL.
- [x] 6.4 Implementar `POST /api/v1/identity-verifications/logout`, invalidación local y limpieza de cookie sin inventar parámetros de logout remoto.
- [x] 6.5 Configurar atributos HttpOnly, SameSite=Lax, Path, duración y Secure por ambiente; rotar identidad-init a flow-auth al verificar.
- [x] 6.6 Añadir protección selectiva para estado y futuras APIs post-identidad, validación contra persistencia, CORS/origen y rechazo de requestId como autorización.
- [x] 6.7 Añadir manejo global de errores ID Perú y pruebas que demuestren ausencia de DNI, state, verifier, code, tokens y secretos en respuestas y logs.
- [x] 6.8 Documentar todos los endpoints, DTO, formularios, cookies, respuestas y errores en OpenAPI, actualizar el snapshot y regenerar tipos TypeScript.

## 7. Interfaz del paso 1

- [x] 7.1 Implementar `/verificacion-identidad` basada en `step-1.png`, corrigiendo el stepper a cinco pasos y explicando límites/continuación del paso.
- [x] 7.2 Implementar inicio de autenticación con una sola solicitud activa, estado busy accesible y navegación completa a la URL backend.
- [x] 7.3 Implementar `/verificacion-identidad/retorno` con procesamiento y consulta de estado mediante cookie, sin parámetros sensibles.
- [x] 7.4 Implementar resultados accesibles de éxito, cancelación, rechazo, mismatch, expiración, timeout e indisponibilidad, con reintento solo cuando sea seguro.
- [x] 7.5 Adaptar el cliente HTTP a `credentials: include`, errores generados y cancelación de trabajo al desmontar, sin leer ni persistir cookies/tokens.
- [x] 7.6 Añadir pruebas de componentes, teclado, foco, responsive, doble envío, retorno y bloqueo sin continuidad o autorización.

## 8. Validación integral y entrega

- [x] 8.1 Ejecutar pruebas backend unitarias e integración MySQL, incluyendo flujo mock completo hasta habilitación del paso 2.
- [x] 8.2 Ejecutar compilación Maven, validación Flyway/Hibernate, OpenAPI y comprobación de que la consulta SPEC-10/reCAPTCHA no regresa.
- [x] 8.3 Ejecutar instalación reproducible, lint, typecheck, pruebas y build de Next.js.
- [x] 8.4 Ejecutar una prueba navegador de extremo a extremo en modo mock para éxito y principales fallos, verificando cookies y ausencia de datos sensibles.
- [x] 8.5 Ejecutar el adaptador real contra el servidor controlado y documentar evidencia de compatibilidad con el protocolo v1.2.
- [x] 8.6 Realizar la prueba manual institucional únicamente si existen convenio y credenciales autorizadas; si faltan, dejarla explícitamente pendiente sin usar credenciales del proyecto de referencia.
- [x] 8.7 Revisar el diff final para confirmar que no se implementaron listado, selección, motivo, revocación, constancia, sesión permanente ni recuperación de trámites.
