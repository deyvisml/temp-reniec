ALTER TABLE identity_verification
    ADD COLUMN provider_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Modo real o simulado utilizado por el proveedor de identidad' AFTER provider,
    ADD COLUMN state_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Hash SHA-256 del state OAuth, nunca el valor enviado al proveedor' AFTER external_reference,
    ADD COLUMN state_expires_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC hasta la que el state puede utilizarse' AFTER state_hash,
    ADD COLUMN state_consumed_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC en que el callback consumió el state' AFTER state_expires_at,
    ADD COLUMN pkce_verifier_protected VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Code verifier PKCE cifrado temporalmente y eliminado al finalizar el intento' AFTER state_consumed_at,
    ADD COLUMN provider_session_state VARCHAR(256) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Referencia session_state devuelta por ID Perú para la transacción' AFTER pkce_verifier_protected,
    ADD COLUMN verified_subject_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Hash del identificador técnico sub validado, sin datos personales' AFTER provider_session_state,
    ADD COLUMN authorization_jti_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Hash del identificador de la autorización temporal emitida' AFTER verified_subject_hash,
    ADD COLUMN authorization_expires_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC de expiración de la autorización temporal' AFTER authorization_jti_hash,
    ADD COLUMN authorization_invalidated_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC de invalidación anticipada de la autorización' AFTER authorization_expires_at,
    ADD COLUMN authorization_invalidation_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Motivo técnico normalizado de invalidación de la autorización' AFTER authorization_invalidated_at,
    ADD COLUMN updated_at TIMESTAMP(6) NULL
        COMMENT 'Fecha y hora UTC de la última actualización del intento' AFTER created_at,
    ADD CONSTRAINT uq_identity_state_hash UNIQUE (state_hash),
    ADD CONSTRAINT chk_identity_state_dates CHECK (
        state_consumed_at IS NULL OR (state_expires_at IS NOT NULL AND state_consumed_at >= started_at)
    ),
    ADD CONSTRAINT chk_identity_authorization_dates CHECK (
        authorization_invalidated_at IS NULL OR authorization_expires_at IS NOT NULL
    ),
    ADD INDEX idx_identity_state_lookup (state_hash, verification_status, state_consumed_at, state_expires_at),
    ADD INDEX idx_identity_authorization (request_id, authorization_jti_hash, authorization_expires_at);

UPDATE identity_verification SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE identity_verification
    MODIFY COLUMN updated_at TIMESTAMP(6) NOT NULL
        COMMENT 'Fecha y hora UTC de la última actualización del intento';
