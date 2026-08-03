ALTER TABLE revocation_request_digital_credential
    ADD COLUMN revoked_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que la credencial digital fue revocada'
        AFTER selected_at;

UPDATE revocation_request_digital_credential digital_credential
SET revoked_at = (
    SELECT MAX(operation.completed_at)
    FROM revocation_operation operation
    WHERE operation.request_id = digital_credential.request_id
      AND operation.normalized_result = 'SUCCEEDED'
      AND operation.completed_at IS NOT NULL
)
WHERE digital_credential.availability_status = 'REVOKED';

ALTER TABLE revocation_request_digital_credential
    ADD CONSTRAINT chk_revocation_request_digital_credential_revoked_at CHECK (
        (availability_status = 'REVOKED' AND revoked_at IS NOT NULL AND revoked_at >= emission_created_at)
        OR (availability_status <> 'REVOKED' AND revoked_at IS NULL)
    );
