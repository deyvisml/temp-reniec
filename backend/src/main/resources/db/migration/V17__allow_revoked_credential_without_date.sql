ALTER TABLE revocation_request_digital_credential
    DROP CHECK chk_revocation_request_digital_credential_revoked_at,
    MODIFY COLUMN revoked_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC opcional informada por el proveedor para una credencial revocada';

ALTER TABLE revocation_request_digital_credential
    ADD CONSTRAINT chk_revocation_request_digital_credential_revoked_at CHECK (
        (availability_status = 'REVOKED' AND (revoked_at IS NULL OR revoked_at >= emission_created_at))
        OR (availability_status <> 'REVOKED' AND revoked_at IS NULL)
    );
