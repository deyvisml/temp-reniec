ALTER TABLE certificate_cancellation_request
    COMMENT = 'Solicitud ciudadana que concentra el estado y resultado actual del trámite de cancelación';

ALTER TABLE certificate_cancellation_request
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la solicitud de cancelación',
    MODIFY COLUMN dni CHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Número de DNI asociado a la solicitud ciudadana',
    MODIFY COLUMN request_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado actual de avance de la solicitud controlado por el backend',
    MODIFY COLUMN eligibility_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Resultado vigente de la consulta de certificados disponibles',
    MODIFY COLUMN reason_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Código del motivo de cancelación elegido por el ciudadano',
    MODIFY COLUMN other_reason VARCHAR(300) NULL
        COMMENT 'Descripción ingresada cuando el motivo seleccionado es otro',
    MODIFY COLUMN confirmed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que el ciudadano confirmó la cancelación',
    MODIFY COLUMN final_outcome VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado general final de la solicitud de cancelación',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación de la solicitud',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización de la solicitud';

ALTER TABLE certificate_eligibility_check
    COMMENT = 'Intento de consulta que determina los certificados vigentes disponibles para una solicitud';

ALTER TABLE certificate_eligibility_check
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del intento de consulta de elegibilidad',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación a la que pertenece el intento',
    MODIFY COLUMN attempt_number INT UNSIGNED NOT NULL
        COMMENT 'Número secuencial del intento dentro de la solicitud',
    MODIFY COLUMN check_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado técnico actual del intento de consulta',
    MODIFY COLUMN normalized_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado normalizado obtenido de la consulta externa',
    MODIFY COLUMN external_reference VARCHAR(128) NULL
        COMMENT 'Referencia técnica opcional asignada por el servicio externo',
    MODIFY COLUMN requested_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que se inició la consulta',
    MODIFY COLUMN responded_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que se recibió el resultado de la consulta',
    MODIFY COLUMN error_code VARCHAR(64) NULL
        COMMENT 'Código técnico normalizado cuando la consulta no finaliza correctamente',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación para rastrear la consulta en logs técnicos',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del registro de consulta';

ALTER TABLE identity_verification
    COMMENT = 'Intento de verificación de identidad realizado para una solicitud de cancelación';

ALTER TABLE identity_verification
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del intento de verificación de identidad',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación a la que pertenece la verificación',
    MODIFY COLUMN attempt_number INT UNSIGNED NOT NULL
        COMMENT 'Número secuencial del intento de verificación dentro de la solicitud',
    MODIFY COLUMN provider VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Código del proveedor utilizado para verificar la identidad',
    MODIFY COLUMN verification_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado actual del intento de verificación de identidad',
    MODIFY COLUMN external_reference VARCHAR(128) NULL
        COMMENT 'Referencia técnica opcional asignada por el proveedor de identidad',
    MODIFY COLUMN dni_match_result VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Resultado de correspondencia entre la identidad verificada y el DNI solicitado',
    MODIFY COLUMN started_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de inicio de la verificación',
    MODIFY COLUMN completed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC de finalización de la verificación',
    MODIFY COLUMN error_or_cancellation_code VARCHAR(64) NULL
        COMMENT 'Código normalizado de error o cancelación del intento',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación para rastrear la verificación en logs técnicos',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del registro de verificación';

ALTER TABLE revocation_operation
    COMMENT = 'Operación técnica e idempotente enviada para revocar certificados de una solicitud';

ALTER TABLE revocation_operation
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la operación técnica de revocación',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación que origina la operación de revocación',
    MODIFY COLUMN idempotency_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Clave única que evita ejecutar dos veces la misma operación técnica',
    MODIFY COLUMN attempt_number INT UNSIGNED NOT NULL
        COMMENT 'Número secuencial del intento técnico dentro de la solicitud',
    MODIFY COLUMN operation_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado actual de ejecución de la operación de revocación',
    MODIFY COLUMN external_reference VARCHAR(128) NULL
        COMMENT 'Referencia técnica opcional asignada por el servicio de revocación',
    MODIFY COLUMN prepared_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que se preparó la operación',
    MODIFY COLUMN submitted_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que la operación fue enviada al servicio externo',
    MODIFY COLUMN responded_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que se recibió una respuesta externa',
    MODIFY COLUMN completed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que la operación alcanzó un resultado final',
    MODIFY COLUMN normalized_result VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado general normalizado de la operación técnica',
    MODIFY COLUMN error_code VARCHAR(64) NULL
        COMMENT 'Código técnico normalizado cuando la operación presenta un error',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación para rastrear la operación en logs técnicos',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación de la operación',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización de la operación';

ALTER TABLE cancellation_receipt
    COMMENT = 'Constancia generada como evidencia del resultado confirmado de una solicitud';

ALTER TABLE cancellation_receipt
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la constancia de cancelación',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación respaldada por la constancia',
    MODIFY COLUMN revocation_operation_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Operación de revocación confirmada que sustenta la constancia',
    MODIFY COLUMN receipt_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Código único asignado a la constancia',
    MODIFY COLUMN generation_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado actual de generación y disponibilidad de la constancia',
    MODIFY COLUMN storage_reference VARCHAR(256) NULL
        COMMENT 'Referencia al archivo de constancia almacenado fuera de MySQL',
    MODIFY COLUMN generated_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que se generó la constancia',
    MODIFY COLUMN available_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC desde la que la constancia quedó disponible',
    MODIFY COLUMN error_code VARCHAR(64) NULL
        COMMENT 'Código técnico normalizado cuando falla la generación de la constancia',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del registro de constancia',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización de la constancia';

ALTER TABLE cancellation_audit_event
    COMMENT = 'Evento cronológico de auditoría asociado al ciclo de vida de una solicitud';

ALTER TABLE cancellation_audit_event
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del evento de auditoría',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación relacionada con el evento',
    MODIFY COLUMN event_type VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Tipo de evento relevante registrado por el backend',
    MODIFY COLUMN previous_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Estado de la solicitud antes del evento cuando corresponde',
    MODIFY COLUMN new_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Estado de la solicitud después del evento cuando corresponde',
    MODIFY COLUMN result VARCHAR(64) NULL
        COMMENT 'Resultado breve asociado al evento cuando corresponde',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación para rastrear el evento en logs técnicos',
    MODIFY COLUMN event_origin VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Origen funcional o técnico que produjo el evento',
    MODIFY COLUMN occurred_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que ocurrió el evento';

ALTER TABLE cancellation_request_certificate
    COMMENT = 'Certificado vigente obtenido para una solicitud y decisión actual de selección';

ALTER TABLE cancellation_request_certificate
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del certificado asociado a la solicitud',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación propietaria del certificado consultado',
    MODIFY COLUMN eligibility_check_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Intento de consulta que obtuvo este certificado',
    MODIFY COLUMN order_number VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Número de orden de la emisión conservado como texto',
    MODIFY COLUMN emission_created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación de la emisión del certificado',
    MODIFY COLUMN certificate_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador UUID canónico del certificado devuelto por el servicio',
    MODIFY COLUMN availability_status VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Disponibilidad actual del certificado dentro de la solicitud',
    MODIFY COLUMN consulted_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que el certificado fue obtenido en la consulta',
    MODIFY COLUMN selected BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Indica si el ciudadano seleccionó el certificado para cancelarlo',
    MODIFY COLUMN selected_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que el certificado fue seleccionado',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Versión usada para detectar actualizaciones concurrentes de la selección',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del registro del certificado',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización del certificado';

ALTER TABLE certificate_revocation_result
    COMMENT = 'Resultado técnico individual de un certificado dentro de una operación de revocación';

ALTER TABLE certificate_revocation_result
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del resultado individual de revocación',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de cancelación a la que pertenece el resultado',
    MODIFY COLUMN revocation_operation_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Operación técnica que produjo el resultado individual',
    MODIFY COLUMN request_certificate_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Certificado de la solicitud al que corresponde el resultado',
    MODIFY COLUMN submitted_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'UUID exacto del certificado enviado a revocación',
    MODIFY COLUMN result_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado normalizado del resultado individual del certificado',
    MODIFY COLUMN result_code VARCHAR(64) NULL
        COMMENT 'Código normalizado devuelto para el resultado individual',
    MODIFY COLUMN result_message VARCHAR(256) NULL
        COMMENT 'Mensaje normalizado asociado al resultado individual',
    MODIFY COLUMN external_reference VARCHAR(128) NULL
        COMMENT 'Referencia técnica opcional asignada por el servicio de revocación',
    MODIFY COLUMN processed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que se confirmó el resultado individual',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación para rastrear el resultado en logs técnicos',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Versión usada para detectar actualizaciones concurrentes del resultado',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del resultado individual',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización del resultado individual';
