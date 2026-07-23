## 1. Ruta canónica y composición del flujo

- [x] 1.1 Crear una definición compartida para la ruta canónica `/cancelacion` y eliminar literales de rutas por paso en los componentes activos.
- [x] 1.2 Crear la página `/cancelacion` y un coordinador sencillo que componga la consulta inicial y la autenticación según el resultado inmediato y el contexto temporal validado por el backend.
- [x] 1.3 Convertir `/`, `/verificacion-identidad` y `/verificacion-identidad/retorno` en redirecciones controladas hacia `/cancelacion`, sin mantener implementaciones duplicadas.
- [x] 1.4 Adaptar la continuación posterior a una disponibilidad positiva para mostrar la autenticación dentro de `/cancelacion` sin incluir `requestId`, DNI, paso ni datos de certificados en la URL.

## 2. Retorno de ID Perú y estados controlados

- [x] 2.1 Cambiar la URL frontend posterior al callback de ID Perú para que termine en `/cancelacion`, conservando el callback OAuth en el backend.
- [x] 2.2 Resolver en la ruta canónica los estados de procesamiento, éxito, cancelación, rechazo y expiración mediante el contexto temporal del backend, sin parámetros de resultado en la URL.
- [x] 2.3 Verificar que el acceso directo o la recarga sin contexto vigente muestre el inicio y que un trámite finalizado o expirado no restaure una pantalla histórica.
- [x] 2.4 Actualizar ejemplos y configuración local de `ID_PERU_FRONTEND_RETURN_URI` para usar `http://localhost:3000/cancelacion`, sin incorporar secretos.

## 3. Pruebas y documentación

- [x] 3.1 Actualizar las pruebas del formulario para comprobar que una respuesta positiva cambia la vista sin abandonar `/cancelacion` y que los demás resultados bloquean la continuación.
- [x] 3.2 Añadir pruebas de rutas para `/`, las rutas antiguas y `/cancelacion`, comprobando redirección canónica y ausencia de parámetros sensibles o técnicos.
- [x] 3.3 Actualizar las pruebas del retorno de ID Perú y del contexto temporal para verificar éxito, errores controlados, recarga, expiración y ausencia de recuperación histórica.
- [x] 3.4 Ejecutar pruebas, lint, comprobación de tipos y compilación del frontend, además de las pruebas backend afectadas por la URL de retorno.
- [x] 3.5 Actualizar la documentación del frontend, variables de entorno e integración ID Perú para establecer `/cancelacion` como única URL ciudadana y retirar referencias vigentes a rutas por paso.
