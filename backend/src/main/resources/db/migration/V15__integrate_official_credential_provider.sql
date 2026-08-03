ALTER TABLE revocation_request_digital_credential
    RENAME COLUMN order_number TO legacy_order_number;

ALTER TABLE revocation_request_digital_credential
    MODIFY COLUMN legacy_order_number VARCHAR(64) NULL COMMENT 'Orden histórica del proveedor anterior; no se usa para nuevas revocaciones',
    ADD COLUMN status_list_index INT UNSIGNED NULL COMMENT 'Índice oficial e inmutable de la credencial en la lista del proveedor' AFTER legacy_order_number,
    ADD COLUMN credential_type VARCHAR(100) NULL COMMENT 'Tipo de credencial informado por el proveedor oficial' AFTER status_list_index,
    ADD COLUMN provider_credential_status INT UNSIGNED NULL COMMENT 'Estado crudo validado del proveedor: 0 vigente, 1 revocada' AFTER credential_type,
    ADD CONSTRAINT uq_revocation_request_credential_status_index UNIQUE (request_id, status_list_index),
    ADD CONSTRAINT chk_request_credential_provider_status CHECK (provider_credential_status IS NULL OR provider_credential_status IN (0, 1));

ALTER TABLE revocation_operation
    ADD COLUMN provider_credential_status INT UNSIGNED NULL COMMENT 'Estado crudo devuelto por la operación oficial: 0 vigente, 1 revocada' AFTER normalized_result,
    ADD CONSTRAINT chk_revocation_operation_provider_status CHECK (provider_credential_status IS NULL OR provider_credential_status IN (0, 1));
