ALTER TABLE cancellation_request_certificate
    DROP FOREIGN KEY fk_request_certificate_eligibility,
    DROP INDEX idx_request_certificate_source,
    DROP COLUMN eligibility_check_id;

ALTER TABLE certificate_eligibility_check
    DROP FOREIGN KEY fk_eligibility_request,
    DROP INDEX uq_eligibility_request_identity,
    DROP INDEX uq_eligibility_attempt,
    DROP INDEX idx_eligibility_latest,
    DROP CHECK chk_eligibility_attempt,
    DROP CHECK chk_eligibility_response_time;

RENAME TABLE certificate_eligibility_check TO certificate_availability_check;

ALTER TABLE certificate_cancellation_request
    RENAME COLUMN eligibility_result TO availability_result;

UPDATE certificate_cancellation_request
SET availability_result = CASE availability_result
    WHEN 'ELIGIBLE' THEN 'AVAILABLE'
    WHEN 'NOT_ELIGIBLE' THEN 'NOT_AVAILABLE'
    ELSE availability_result
END;

UPDATE certificate_cancellation_request
SET request_status = CASE request_status
    WHEN 'CHECKING_ELIGIBILITY' THEN 'CHECKING_AVAILABILITY'
    WHEN 'NOT_ELIGIBLE' THEN 'NO_CERTIFICATES_AVAILABLE'
    WHEN 'ELIGIBLE' THEN 'PENDING_IDENTITY_VERIFICATION'
    ELSE request_status
END;

UPDATE certificate_availability_check
SET normalized_result = CASE normalized_result
    WHEN 'ELIGIBLE' THEN 'AVAILABLE'
    WHEN 'NOT_ELIGIBLE' THEN 'NOT_AVAILABLE'
    ELSE normalized_result
END;

ALTER TABLE certificate_availability_check
    ADD CONSTRAINT fk_availability_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    ADD CONSTRAINT uq_availability_attempt UNIQUE (request_id, attempt_number),
    ADD CONSTRAINT chk_availability_attempt CHECK (attempt_number > 0),
    ADD CONSTRAINT chk_availability_response_time
        CHECK (responded_at IS NULL OR responded_at >= requested_at),
    ADD INDEX idx_availability_latest (request_id, attempt_number DESC),
    COMMENT = 'Intentos del servicio inicial que confirma solamente la existencia de certificados disponibles';

ALTER TABLE certificate_cancellation_request
    MODIFY COLUMN availability_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Resultado normalizado de existencia: disponible, no disponible o no determinado';

ALTER TABLE certificate_availability_check
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del intento de consulta de existencia',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud a la que pertenece el intento de consulta de existencia',
    MODIFY COLUMN attempt_number INT UNSIGNED NOT NULL
        COMMENT 'Número secuencial del intento dentro de la solicitud',
    MODIFY COLUMN check_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Estado técnico del intento de consulta',
    MODIFY COLUMN normalized_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado normalizado sin lista ni datos individuales de certificados',
    MODIFY COLUMN external_reference VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Referencia técnica opcional devuelta por el proveedor',
    MODIFY COLUMN requested_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que se inició la consulta',
    MODIFY COLUMN responded_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que finalizó la consulta',
    MODIFY COLUMN error_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Código técnico normalizado cuando el intento falla',
    MODIFY COLUMN correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador de correlación de la solicitud HTTP',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT 'Fecha y hora UTC de creación del registro';

ALTER TABLE cancellation_request_certificate
    COMMENT = 'Certificados obtenidos por el futuro listado posterior a la autenticación y vinculados con la solicitud';
