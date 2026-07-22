## 1. Configuración y dependencia acotada

- [x] 1.1 Añadir y fijar una versión de `@google-recaptcha/react` compatible con Next.js 16/React 19, actualizar el lockfile y comprobar que no incorpore otra arquitectura de formularios o estado.
- [x] 1.2 Incorporar `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` como variable pública con placeholder en `frontend/.env.example`, manteniendo `.env.local` ignorado y sin añadir un modo de bypass al bundle.
- [x] 1.3 Definir propiedades backend validadas para secret, URI oficial, timeout positivo y allowlist no vacía de hostnames; añadir placeholders a `backend/.env.example` sin valores secretos.
- [x] 1.4 Configurar el adaptador Google para ejecución local/real y un doble determinista para pruebas, garantizando que la configuración real incompleta falle al iniciar.

## 2. Verificación anti-bot en el backend

- [x] 2.1 Crear `AntiBotVerificationPort` y excepciones/resultados internos que no expongan DTO, códigos crudos ni transporte de Google al caso de uso.
- [x] 2.2 Implementar el adaptador `siteverify` con `RestClient`, formulario `secret`/`response`, timeouts, parsing defensivo, validación exacta de hostname y clasificación de `timeout-or-duplicate`.
- [x] 2.3 Añadir al formato común los códigos `RECAPTCHA_REQUIRED`, `RECAPTCHA_REJECTED`, `RECAPTCHA_EXPIRED_OR_DUPLICATE`, `RECAPTCHA_UNAVAILABLE`, `RECAPTCHA_TIMEOUT` y `RECAPTCHA_INVALID_RESPONSE`, conservando correlación y mensajes no técnicos.
- [x] 2.4 Ampliar `StartCancellationRequest` con `recaptchaToken` obligatorio y acotado, sin ejemplo real ni posibilidad de eco en respuestas.
- [x] 2.5 Cambiar `CancellationRequestInitiationService` para verificar anti-bot antes de `AvailabilityPersistenceCoordinator.prepare` y antes de toda llamada a `CertificateAvailabilityPort`.
- [x] 2.6 Actualizar controlador y documentación OpenAPI con el nuevo cuerpo, secuencia y respuestas CAPTCHA, sin modificar la respuesta funcional de disponibilidad de SPEC-10.

## 3. Widget y ciclo de vida en el frontend

- [x] 3.1 Crear un wrapper pequeño de reCAPTCHA v2 Checkbox con callbacks estables para token, expiración y error, estado de configuración ausente y montaje responsive/accesible.
- [x] 3.2 Integrar el widget en `DniAvailabilityForm`, habilitando envío solo con DNI válido, token actual y ninguna solicitud activa y manteniendo el guard síncrono contra doble envío.
- [x] 3.3 Enviar `recaptchaToken` mediante el cliente tipado y eliminarlo de memoria tras éxito, rechazo, error, timeout, abort o incertidumbre de red, reiniciando/remontando el widget sin reintento automático.
- [x] 3.4 Mapear errores CAPTCHA a mensajes y acciones accesibles separados de los resultados de disponibilidad, preservando foco, teclado, lectores de pantalla y el comportamiento responsive existente.
- [x] 3.5 Verificar que token, DNI y secret no aparezcan en URL, almacenamiento del navegador, cookies persistentes, errores técnicos, logs o estado reutilizable.

## 4. Contrato y documentación

- [x] 4.1 Regenerar `frontend/openapi/backend-api.json` y `frontend/lib/api/generated.ts` desde el backend y endurecer el drift check para exigir `recaptchaToken` y prohibir secretos o ejemplos reutilizables.
- [x] 4.2 Actualizar README de backend/frontend, integración local y decisiones técnicas con versión v2 Checkbox, variables por ambiente, errores, hostnames y orden de validación.
- [x] 4.3 Documentar que la referencia usa `@google-recaptcha/react` 2.4.2, verificación server-to-server y el par oficial de prueba de Google, indicando qué se adaptó y qué complejidad no se copió.
- [x] 4.4 Documentar la prueba manual con claves oficiales de test exclusivamente en `backend/.env` y `frontend/.env.local`, su advertencia visual esperada y su prohibición expresa en producción.
- [x] 4.5 Confirmar documental y estructuralmente que no existen migraciones, columnas, tablas ni repositorios nuevos para CAPTCHA.

## 5. Pruebas backend e integración

- [x] 5.1 Probar propiedades válidas e inválidas, incluyendo secret ausente, timeout no positivo, URI inválida y allowlist vacía, sin imprimir valores sensibles.
- [x] 5.2 Probar el adaptador Google con servidor HTTP controlado para éxito, rechazo, expirado/duplicado, hostname permitido/denegado/ausente, HTTP fallido, timeout, cuerpo nulo y JSON inválido.
- [x] 5.3 Probar el caso de uso con dobles para demostrar que CAPTCHA inválido o técnico nunca ejecuta preparación, persistencia ni consulta de disponibilidad y que CAPTCHA válido conserva todos los escenarios de SPEC-10.
- [x] 5.4 Ampliar pruebas HTTP/OpenAPI para token ausente, códigos CAPTCHA, correlación, respuesta sin token y ausencia de DNI/token/secret en logs.
- [x] 5.5 Ejecutar integración con MySQL y adaptador anti-bot simulado para demostrar cero solicitudes/intentos ante rechazo y flujo normal ante aceptación, sin dependencia de Google.

## 6. Pruebas frontend y validación final

- [x] 6.1 Probar lógica del formulario y wrapper con dobles controlados para widget pendiente, token válido, expiración, error de script, configuración ausente, reset después del uso y doble envío.
- [x] 6.2 Probar el contrato cliente para envío conjunto DNI/token, errores anti-bot, descarte de evidencia y conservación de resultados positivos, negativos e inconclusos de SPEC-10.
- [ ] 6.3 Comprobar manualmente el widget real con las claves oficiales de prueba, navegación por teclado, foco, tamaños móvil/escritorio y reinicio tras cada intento.
- [x] 6.4 Ejecutar pruebas Maven rápidas y completas con Testcontainers, pruebas Vitest, typecheck, build Next.js, sincronización OpenAPI, comprobación de contrato, validación OpenSpec estricta y `git diff --check`.
