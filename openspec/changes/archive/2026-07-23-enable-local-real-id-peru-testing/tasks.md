## 1. Configuración local de ID Perú

- [x] 1.1 Permitir seleccionar `mock` o `real` mediante `ID_PERU_MODE` bajo el perfil local, conservando `mock` como valor predeterminado.
- [x] 1.2 Incorporar en la configuración local las propiedades externas mínimas del adaptador real sin exponer secretos.
- [x] 1.3 Ajustar la validación para aceptar URLs HTTP de `localhost` solo en desarrollo real y mantener HTTPS para el proveedor y toda configuración productiva.

## 2. Mock de disponibilidad para el flujo normal

- [x] 2.1 Mantener fixtures deterministas para escenarios alternativos y devolver `AVAILABLE` para cualquier DNI válido no reservado.
- [x] 2.2 Actualizar las pruebas del adaptador y del caso de uso para cubrir un DNI normal y todos los fixtures especiales.

## 3. Pruebas de configuración

- [x] 3.1 Verificar mediante pruebas que local usa mock por defecto y puede activar el adaptador real.
- [x] 3.2 Verificar valores requeridos, URLs locales permitidas, proveedor HTTPS y producción obligatoriamente real.
- [x] 3.3 Comprobar que no se activan simultáneamente adaptadores incompatibles.

## 4. Documentación operativa

- [x] 4.1 Actualizar `backend/.env.example` con modos local mock/real y placeholders no sensibles.
- [x] 4.2 Documentar en el README del backend los pasos exactos para ambos modos y la tabla actualizada de DNI de prueba.
- [x] 4.3 Actualizar la documentación de ID Perú con callback, credenciales, flujo esperado y diagnóstico de errores.

## 5. Validación final

- [x] 5.1 Ejecutar pruebas unitarias relevantes y la suite completa del backend.
- [x] 5.2 Validar el cambio con OpenSpec y confirmar que todos los artefactos coinciden con la implementación.

## 6. Compatibilidad con las URLs registradas de prueba

- [x] 6.1 Configurar el callback uniforme `/api/v1/idperu/callback` y, en `local`, el retorno `/autorizacion`, preservando el retorno productivo `/cancelacion`.
- [x] 6.2 Incorporar inicialmente `/autorizacion` como retorno compatible, sin duplicar la interfaz ni transportar datos en la URL.
- [x] 6.3 Permitir el referer HTTP registrado solo para localhost local y actualizar pruebas y documentación operativa.
- [x] 6.4 Ejecutar las validaciones completas de backend, frontend y OpenSpec.

## 7. Callback uniforme

- [x] 7.1 Utilizar `/api/v1/idperu/callback` en todos los ambientes y eliminar su configuración redundante por perfil.
- [x] 7.2 Actualizar contratos, pruebas y documentación, y validar el cambio completo.

## 8. Raíz institucional fija

- [x] 8.1 Fijar `https://idaas2.reniec.gob.pe/` en la configuración versionada y retirar `ID_PERU_BASE_URI` de local, producción y `.env.example`.
- [x] 8.2 Actualizar pruebas y documentación, y validar que el modo real solo requiera credenciales externas.

## 9. Ruta local real del paso 1

- [x] 9.1 Mostrar el paso 1 en `/autorizacion` durante local, navegar allí tras una consulta positiva y conservar `/cancelacion` en producción reutilizando el flujo existente.
- [x] 9.2 Actualizar pruebas y documentación para eliminar la redirección local inmediata y validar frontend y OpenSpec.

## 10. Codificación de la autorización ID Perú

- [x] 10.1 Construir la URL real mediante expansión codificada de variables para proteger `redirect_uri`, `state` y `vd` sin doble codificación.
- [x] 10.2 Agregar pruebas sobre la consulta cruda y ejecutar las validaciones del backend y OpenSpec.
