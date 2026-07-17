## 1. Dependencias y configuración

- [x] 1.1 Añadir al `pom.xml` las dependencias gestionadas de JPA, Flyway/MySQL, Connector/J y Testcontainers, sin incorporar otra base de datos o herramienta preventiva
- [x] 1.2 Configurar Maven Failsafe para ejecutar clases `*IT` en `verify`, manteniendo `mvn test` como suite rápida sin contenedores
- [x] 1.3 Configurar datasource, Flyway, `ddl-auto=validate`, `open-in-view=false`, UTC y health agregado mediante los perfiles existentes y las cinco variables `DB_*`
- [x] 1.4 Verificar que usuario y contraseña sean obligatorios en ejecución local, que Flyway clean permanezca deshabilitado y que no exista una credencial en archivos rastreados

## 2. Migración inicial

- [x] 2.1 Crear `V1__create_cancellation_persistence.sql` con `cancellation_process`, UUID interno, referencia segura del DNI, últimos cuatro dígitos, estado, actividad, timestamps, expiración y versión
- [x] 2.2 Añadir en la misma migración `cancellation_session` con FK al proceso, referencia única irreversible, creación, expiración e invalidación, sin cascada de borrado
- [x] 2.3 Incorporar únicamente restricciones e índices para integridad temporal, formatos, búsqueda de proceso vigente, FK y referencia única de sesión
- [x] 2.4 Revisar que la migración sea reproducible desde una base vacía y no contenga estados enumerados, datos semilla, credenciales, procedimientos, triggers, archivos o campos futuros

## 3. Modelo y repositorios

- [x] 3.1 Crear el enum con los seis estados iniciales y comportamiento mínimo para distinguir estados activos y terminales, sin una máquina completa de transiciones
- [x] 3.2 Implementar la entidad de proceso con validación de referencias, UUID, timestamps UTC, expiración, `@Version` y un método explícito para cambiar estado y actividad
- [x] 3.3 Implementar la entidad de sesión asociada al proceso con referencia irreversible, vigencia e invalidación, sin tokens, cookies o lógica de autenticación
- [x] 3.4 Crear el repositorio de procesos con operaciones JPA estándar y consulta del proceso activo no expirado más reciente por referencia segura
- [x] 3.5 Crear el repositorio de sesiones con persistencia y búsqueda por referencia única, sin repositorios genéricos, servicios, controladores, ports o adaptadores vacíos

## 4. Pruebas rápidas e integración MySQL

- [x] 4.1 Adaptar las pruebas técnicas existentes para excluir explícitamente datasource/JPA/Flyway y confirmar que `mvn test` sigue funcionando solo con Java 21
- [x] 4.2 Crear soporte Testcontainers reutilizable con MySQL y conexión dinámica para las clases `*IT`, sin H2, MySQL instalado ni datos personales reales
- [x] 4.3 Probar el arranque completo desde una base vacía, la ejecución/validación de Flyway, el esquema JPA y el health `UP` sin detalles sensibles
- [x] 4.4 Probar creación, recuperación, cambio de estado, coherencia de actividad, timestamps y búsqueda que excluye procesos expirados o inactivos
- [x] 4.5 Probar múltiples sesiones por proceso, invalidación, búsqueda por referencia y restricciones de FK, unicidad, nulabilidad y formato
- [x] 4.6 Probar que dos actualizaciones concurrentes del mismo proceso producen un conflicto de bloqueo optimista y no una sobrescritura silenciosa
- [x] 4.7 Probar o verificar controladamente que la pérdida de MySQL produce health no saludable sin revelar URL, usuario, SQL, stack trace o credenciales

## 5. Documentación

- [x] 5.1 Actualizar `backend/README.md` con MySQL compatible, creación local de base y usuario con privilegios mínimos, variables, arranque y migraciones automáticas
- [x] 5.2 Documentar `mvn test`, `mvn verify`, el requisito de runtime de contenedores y un reinicio local seguro que no se aplique a ambientes compartidos
- [x] 5.3 Registrar que `dni_reference_hash` requiere después HMAC o pseudonimización institucional, que no existe ciphertext todavía y que la tabla de sesiones no implementa JWT ni recuperación
- [x] 5.4 Mantener explícitamente diferidos producción, backups, retención, cifrado institucional, auditoría funcional y despliegue

## 6. Verificación final

- [x] 6.1 Ejecutar `mvn test` sin Docker/MySQL y confirmar que pasan las pruebas técnicas rápidas
- [x] 6.2 Ejecutar `mvn verify` con un runtime de contenedores y confirmar migraciones, integración MySQL, empaquetado y limpieza del contenedor
- [x] 6.3 Revisar el árbol de dependencias para confirmar que MySQL es la única base y que no se añadieron seguridad, mensajería, caché u otras dependencias fuera de alcance
- [x] 6.4 Revisar migraciones, entidades, paquetes y rutas para confirmar que existen exactamente dos tablas justificadas, ningún endpoint funcional y ninguna modificación de `/frontend` o los diseños
- [x] 6.5 Ejecutar la validación estricta de OpenSpec y confirmar que todos los artefactos permanecen consistentes con la implementación prevista
