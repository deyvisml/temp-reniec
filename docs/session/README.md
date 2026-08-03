# Sesión transaccional del flujo ciudadano

La portada `/` es pública. Después de que reCAPTCHA y la consulta inicial confirman `AVAILABLE`, el backend crea una única `revocation_flow_session` vinculada a la solicitud y entrega dos cookies `HttpOnly`:

- `revocacion_access`: JWT corto, usado para autorizar APIs internas.
- `revocacion_refresh`: JWT rotatorio, usado únicamente en `POST /api/v1/session/refresh`.

Ambos JWT contienen solo `sid`, `rid`, `jti`, tipo y claims criptográficos. El refresh añade familia y versión. No contienen DNI, credenciales ni otros datos personales. MySQL guarda hashes de refresh, nunca los tokens.

## Ciclo de vida

1. `AVAILABLE` crea la sesión en estado `PENDING_IDENTITY`.
2. `/revocacion` y la variante local `/autorizacion` validan la sesión en el backend antes de renderizar.
3. El callback exitoso de ID Perú eleva la misma sesión a `IDENTITY_VERIFIED`.
4. El access dura 15 minutos y puede actualizarse mientras el refresh rotatorio de 3 días siga vigente. Cada rotación válida actualiza esa ventana, usa bloqueo de fila y conserva brevemente el hash anterior para reconocer carreras legítimas entre pestañas; una reutilización real invalida la familia.
5. Logout invalida la sesión, elimina ambas cookies y marca `ABANDONED` solo una solicitud todavía reversible.

La sesión conserva únicamente la operación actual. No recupera solicitudes finalizadas, selecciones antiguas ni constancias anteriores.

## Seguridad y ambientes

Las cookies usan `SameSite=Lax`, `Path=/` y `Secure` fuera de local/test. Las mutaciones autenticadas validan el `Origin` exacto permitido; el callback de ID Perú se protege con `state` de un solo uso. Producción exige `SESSION_SIGNING_SECRET` externo en Base64 con al menos 32 bytes. El valor incluido para local es público, exclusivo de desarrollo y no debe reutilizarse.

## Prueba local

1. Levanta MySQL y el backend con perfil `local`.
2. Levanta el frontend en `http://localhost:3000`.
3. Inicia desde `/` con un resultado positivo.
4. Confirma que `/revocacion` o `/autorizacion` cargue al recargar, que otra pestaña vea la misma operación y que `Cerrar sesión` lleve todas las pestañas a la portada.

Los tokens no deben inspeccionarse ni copiarse desde JavaScript: al ser `HttpOnly`, esa restricción es parte del diseño.
