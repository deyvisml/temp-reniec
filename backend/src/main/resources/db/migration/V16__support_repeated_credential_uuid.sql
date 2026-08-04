ALTER TABLE revocation_request_digital_credential
    DROP INDEX uq_revocation_request_digital_credential_uuid,
    ADD CONSTRAINT uq_revocation_request_credential_identity
        UNIQUE (request_id, digital_credential_uuid, status_list_index),
    MODIFY COLUMN digital_credential_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT 'UUID informado por el proveedor; puede repetirse y solo identifica una credencial junto con status_list_index';
