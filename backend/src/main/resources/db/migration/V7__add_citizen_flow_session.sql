CREATE TABLE cancellation_flow_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno de la sesión transaccional',
    request_id BIGINT UNSIGNED NOT NULL COMMENT 'Solicitud activa vinculada de forma exclusiva a la sesión',
    session_status VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Estado controlado de la sesión: PENDING_IDENTITY, IDENTITY_VERIFIED, INVALIDATED o EXPIRED',
    refresh_family CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Familia técnica usada para rotar y detectar reutilización del refresh token',
    refresh_version INT NOT NULL COMMENT 'Versión vigente del refresh token dentro de su familia',
    current_refresh_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Hash SHA-256 del refresh token vigente; nunca se almacena el token',
    previous_refresh_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Hash anterior conservado brevemente para distinguir concurrencia de reutilización',
    previous_valid_until TIMESTAMP(6) NULL COMMENT 'Fin de la ventana breve de concurrencia del refresh anterior',
    refresh_expires_at TIMESTAMP(6) NOT NULL COMMENT 'Expiración absoluta de la sesión activa en UTC',
    last_used_at TIMESTAMP(6) NOT NULL COMMENT 'Fecha y hora UTC del último uso válido',
    invalidated_at TIMESTAMP(6) NULL COMMENT 'Fecha y hora UTC de invalidación anticipada',
    invalidation_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Motivo técnico normalizado de invalidación',
    created_at TIMESTAMP(6) NOT NULL COMMENT 'Fecha y hora UTC de creación',
    updated_at TIMESTAMP(6) NOT NULL COMMENT 'Fecha y hora UTC de última actualización',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Versión para control de concurrencia optimista',
    PRIMARY KEY (id),
    CONSTRAINT uq_flow_session_request UNIQUE (request_id),
    CONSTRAINT uq_flow_session_family UNIQUE (refresh_family),
    CONSTRAINT fk_flow_session_request FOREIGN KEY (request_id) REFERENCES certificate_cancellation_request (id),
    CONSTRAINT chk_flow_session_version CHECK (refresh_version >= 1),
    CONSTRAINT chk_flow_session_dates CHECK (refresh_expires_at > created_at),
    INDEX idx_flow_session_active (session_status, refresh_expires_at)
) ENGINE=InnoDB COMMENT='Sesión transaccional activa del flujo ciudadano';

ALTER TABLE identity_verification
    DROP INDEX idx_identity_authorization,
    DROP CHECK chk_identity_authorization_dates,
    DROP COLUMN authorization_jti_hash,
    DROP COLUMN authorization_expires_at,
    DROP COLUMN authorization_invalidated_at,
    DROP COLUMN authorization_invalidation_reason;
