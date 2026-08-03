-- V1-V10 are historical and immutable. This migration introduces the current
-- digital-credential revocation terminology without rewriting prior checksums.

ALTER TABLE certificate_availability_check DROP FOREIGN KEY fk_availability_request;
ALTER TABLE identity_verification DROP FOREIGN KEY fk_identity_request;
ALTER TABLE revocation_operation DROP FOREIGN KEY fk_revocation_request;
ALTER TABLE cancellation_receipt
    DROP FOREIGN KEY fk_receipt_request,
    DROP FOREIGN KEY fk_receipt_revocation;
ALTER TABLE cancellation_audit_event DROP FOREIGN KEY fk_audit_request;
ALTER TABLE cancellation_flow_session DROP FOREIGN KEY fk_flow_session_request;
ALTER TABLE cancellation_request_certificate
    DROP FOREIGN KEY fk_request_certificate_request,
    DROP CHECK chk_request_certificate_uuid,
    DROP CHECK chk_request_certificate_selection,
    DROP CHECK chk_request_certificate_consulted;

RENAME TABLE
    certificate_cancellation_request TO digital_credential_revocation_request,
    certificate_availability_check TO digital_credential_availability_check,
    cancellation_request_certificate TO revocation_request_digital_credential,
    cancellation_flow_session TO revocation_flow_session,
    cancellation_receipt TO revocation_receipt,
    cancellation_audit_event TO revocation_audit_event;

ALTER TABLE revocation_request_digital_credential
    RENAME COLUMN certificate_uuid TO digital_credential_uuid,
    RENAME INDEX uq_request_certificate_uuid TO uq_revocation_request_digital_credential_uuid,
    RENAME INDEX idx_request_certificate_list TO idx_revocation_request_digital_credential_list,
    RENAME INDEX idx_request_certificate_selection TO idx_revocation_request_digital_credential_selection,
    RENAME INDEX uq_request_certificate_single_selected TO uq_revocation_request_single_selected;

ALTER TABLE identity_verification
    RENAME COLUMN error_or_cancellation_code TO error_or_revocation_code;

ALTER TABLE digital_credential_revocation_request
    RENAME INDEX idx_request_dni_status_created TO idx_revocation_request_dni_status_created;
ALTER TABLE digital_credential_availability_check
    RENAME INDEX idx_availability_latest TO idx_digital_credential_availability_latest;
ALTER TABLE revocation_receipt
    RENAME INDEX idx_receipt_available TO idx_revocation_receipt_available;
ALTER TABLE revocation_audit_event
    RENAME INDEX idx_audit_history TO idx_revocation_audit_history;
ALTER TABLE revocation_flow_session
    RENAME INDEX idx_flow_session_active TO idx_revocation_flow_session_active;

UPDATE digital_credential_revocation_request
SET request_status = CASE request_status
    WHEN 'NO_CERTIFICATES_AVAILABLE' THEN 'NO_DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'AUTHENTICATED_PENDING_CERTIFICATE_LIST' THEN 'AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CHECKING_CERTIFICATE_LIST' THEN 'CHECKING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CERTIFICATES_AVAILABLE' THEN 'DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'CERTIFICATES_SELECTED' THEN 'DIGITAL_CREDENTIALS_SELECTED'
    ELSE request_status
END,
consent_version = CASE consent_version
    WHEN 'CANCELACION_CERTIFICADOS_V1' THEN 'REVOCACION_CREDENCIALES_DIGITALES_V1'
    ELSE consent_version
END;

UPDATE revocation_audit_event
SET event_type = CASE event_type
    WHEN 'CERTIFICATE_LIST_REQUESTED' THEN 'DIGITAL_CREDENTIAL_LIST_REQUESTED'
    WHEN 'CERTIFICATES_AVAILABLE' THEN 'DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'CERTIFICATES_SELECTED' THEN 'DIGITAL_CREDENTIALS_SELECTED'
    WHEN 'CERTIFICATE_LIST_EMPTY' THEN 'DIGITAL_CREDENTIAL_LIST_EMPTY'
    WHEN 'CERTIFICATE_LIST_FAILED' THEN 'DIGITAL_CREDENTIAL_LIST_FAILED'
    ELSE event_type
END,
previous_status = CASE previous_status
    WHEN 'NO_CERTIFICATES_AVAILABLE' THEN 'NO_DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'AUTHENTICATED_PENDING_CERTIFICATE_LIST' THEN 'AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CHECKING_CERTIFICATE_LIST' THEN 'CHECKING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CERTIFICATES_AVAILABLE' THEN 'DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'CERTIFICATES_SELECTED' THEN 'DIGITAL_CREDENTIALS_SELECTED'
    ELSE previous_status
END,
new_status = CASE new_status
    WHEN 'NO_CERTIFICATES_AVAILABLE' THEN 'NO_DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'AUTHENTICATED_PENDING_CERTIFICATE_LIST' THEN 'AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CHECKING_CERTIFICATE_LIST' THEN 'CHECKING_DIGITAL_CREDENTIAL_LIST'
    WHEN 'CERTIFICATES_AVAILABLE' THEN 'DIGITAL_CREDENTIALS_AVAILABLE'
    WHEN 'CERTIFICATES_SELECTED' THEN 'DIGITAL_CREDENTIALS_SELECTED'
    ELSE new_status
END;

ALTER TABLE digital_credential_availability_check
    ADD CONSTRAINT fk_digital_credential_availability_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id);
ALTER TABLE identity_verification
    ADD CONSTRAINT fk_identity_revocation_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id);
ALTER TABLE revocation_operation
    ADD CONSTRAINT fk_revocation_operation_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id);
ALTER TABLE revocation_receipt
    ADD CONSTRAINT fk_revocation_receipt_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id),
    ADD CONSTRAINT fk_revocation_receipt_operation FOREIGN KEY (revocation_operation_id)
        REFERENCES revocation_operation (id);
ALTER TABLE revocation_audit_event
    ADD CONSTRAINT fk_revocation_audit_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id);
ALTER TABLE revocation_flow_session
    ADD CONSTRAINT fk_revocation_flow_session_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id);
ALTER TABLE revocation_request_digital_credential
    ADD CONSTRAINT fk_revocation_request_digital_credential_request FOREIGN KEY (request_id)
        REFERENCES digital_credential_revocation_request (id),
    ADD CONSTRAINT chk_revocation_request_digital_credential_uuid CHECK (
        REGEXP_LIKE(
            digital_credential_uuid,
            '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
            'c'
        )
    ),
    ADD CONSTRAINT chk_revocation_request_digital_credential_selection CHECK (
        (selected = FALSE AND selected_at IS NULL)
        OR (selected = TRUE AND selected_at IS NOT NULL)
    ),
    ADD CONSTRAINT chk_revocation_request_digital_credential_consulted CHECK (
        consulted_at >= emission_created_at
    );

ALTER TABLE digital_credential_revocation_request
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la solicitud de revocación',
    MODIFY COLUMN availability_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Resultado normalizado de existencia de credenciales digitales',
    MODIFY COLUMN reason_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Código del motivo de revocación elegido por el ciudadano',
    MODIFY COLUMN confirmed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que el ciudadano confirmó la revocación',
    MODIFY COLUMN final_outcome VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado general final de la solicitud de revocación',
    COMMENT = 'Solicitud ciudadana para revocar una credencial digital';
ALTER TABLE digital_credential_availability_check
    MODIFY COLUMN normalized_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Resultado normalizado sin lista ni datos individuales de credenciales digitales',
    COMMENT = 'Consultas de existencia de credenciales digitales disponibles';
ALTER TABLE revocation_request_digital_credential
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la credencial digital asociada a la solicitud',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de revocación propietaria de la credencial digital consultada',
    MODIFY COLUMN emission_created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación de la emisión de la credencial digital',
    MODIFY COLUMN digital_credential_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Identificador UUID canónico de la credencial digital devuelta por el servicio',
    MODIFY COLUMN availability_status VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'Disponibilidad actual de la credencial digital dentro de la solicitud',
    MODIFY COLUMN consulted_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC en que la credencial digital fue obtenida en la consulta',
    MODIFY COLUMN selected BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Indica si el ciudadano seleccionó la credencial digital para revocarla',
    MODIFY COLUMN selected_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que la credencial digital fue seleccionada',
    MODIFY COLUMN created_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de creación del registro de la credencial digital',
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización de la credencial digital',
    COMMENT = 'Credenciales digitales asociadas a una solicitud de revocación';
ALTER TABLE identity_verification
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de revocación a la que pertenece la verificación',
    MODIFY COLUMN error_or_revocation_code VARCHAR(64) NULL
        COMMENT 'Código normalizado de error o interrupción del intento',
    COMMENT = 'Intento de verificación de identidad para una solicitud de revocación';
ALTER TABLE revocation_operation
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud que origina la operación de revocación',
    COMMENT = 'Operación técnica e idempotente para revocar una credencial digital';
ALTER TABLE revocation_flow_session
    COMMENT = 'Sesión autenticada del flujo de revocación';
ALTER TABLE revocation_receipt
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno de la constancia de revocación',
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de revocación respaldada por la constancia',
    COMMENT = 'Constancia generada para una revocación exitosa';
ALTER TABLE revocation_audit_event
    MODIFY COLUMN request_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Solicitud de revocación relacionada con el evento',
    COMMENT = 'Historial de auditoría de una solicitud de revocación';
