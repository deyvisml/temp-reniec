## 1. Corregir el contrato HTTP del callback

- [x] 1.1 Añadir pruebas web que reproduzcan el callback GET real de ID Perú v1 y confirmen que ya no responde `405 METHOD_NOT_ALLOWED`.
- [x] 1.2 Hacer que `/api/v1/idperu/callback` acepte GET con query y POST con formulario, normalizando ambos transportes hacia el mismo caso de uso.
- [x] 1.3 Mantener las validaciones específicas por versión para `code`, `state`, `session_state` y `error`, incluido el consumo atómico y único de state.

## 2. Completar la redirección segura del backend

- [x] 2.1 Convertir éxito, cancelación, rechazo, expiración, mismatch y fallos técnicos controlados en respuestas `303 See Other` hacia la URI frontend fija del ambiente.
- [x] 2.2 Tratar parámetros ausentes, state inválido/repetido y excepciones de callback sin dejar el navegador en un documento JSON ni modificar intentos no identificados de forma segura.
- [x] 2.3 Establecer la autorización temporal únicamente en autenticaciones verificadas y comprobar que las respuestas fallidas no emitan una cookie posidentidad.
- [x] 2.4 Verificar que `Location`, cuerpo, encabezados y logs no contengan code, state, session_state, tokens, DNI, URL completa ni diagnósticos del proveedor.

## 3. Resolver el retorno en el frontend

- [x] 3.1 Hacer que `/cancelacion` y la compatibilidad local `/autorizacion` carguen el mismo orquestador y consulten el estado validado por el backend al volver de ID Perú.
- [x] 3.2 Mostrar después de `VERIFIED` una presentación mínima con el paso 2 activo, sin certificados ficticios, selección ni llamadas al segundo servicio.
- [x] 3.3 Mantener el paso 1 para cancelación, rechazo, mismatch, expiración, timeout, indisponibilidad y error, mostrando un único SweetAlert2 accesible con texto ciudadano y recuperación válida.
- [x] 3.4 Evitar alertas duplicadas por render, refresh o React Strict Mode y prevenir que cerrar un aviso dispare automáticamente una autenticación o navegación.
- [x] 3.5 Ignorar parámetros de URL que intenten forzar el resultado o el paso y regresar a la consulta inicial cuando no exista contexto temporal válido.

## 4. Contrato y documentación

- [x] 4.1 Actualizar OpenAPI para documentar callback GET y POST, parámetros por transporte, redirección 303 y errores resueltos sin cuerpo técnico en el navegador.
- [x] 4.2 Actualizar la documentación de ID Perú y prueba local con el recorrido navegador → callback backend → frontend, incluyendo las rutas configuradas por ambiente.
- [x] 4.3 Registrar la diferencia corregida respecto a la restricción POST anterior y la decisión reutilizada del proyecto institucional de referencia.

## 5. Validación integral

- [x] 5.1 Probar backend para GET exitoso, POST compatible, error del proveedor, state ausente/inválido/expirado/repetido, campos requeridos por v1/v2, cookie y redirección segura.
- [x] 5.2 Probar frontend para transición verificada al paso 2 mínimo, permanencia en paso 1, aviso único por cada fallo, reintento controlado y rutas compartidas.
- [x] 5.3 Ejecutar pruebas completas, compilación y validaciones del backend; ejecutar tests, lint, TypeScript y build del frontend.
- [x] 5.4 Realizar una comprobación manual con ID Perú v1 desde local y confirmar que el navegador termina en `/autorizacion`, no muestra JSON del backend y refleja correctamente éxito o error.
