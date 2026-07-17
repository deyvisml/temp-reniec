CREATE TABLE certificate_cancellation_request (
    id BINARY(16) NOT NULL,
    dni_lookup_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    dni_ciphertext VARBINARY(512) NOT NULL,
    dni_key_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    dni_last_four CHAR(4) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    lifecycle_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    eligibility_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    other_reason_ciphertext VARBINARY(1024) NULL,
    other_reason_key_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    consent_text_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    confirmed_at TIMESTAMP(6) NULL,
    final_outcome VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    recoverable_until TIMESTAMP(6) NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL,
    active_dni_guard CHAR(64) CHARACTER SET ascii COLLATE ascii_bin
        GENERATED ALWAYS AS (
            CASE WHEN lifecycle_status = 'ACTIVE' THEN dni_lookup_hash ELSE NULL END
        ) STORED,
    CONSTRAINT pk_certificate_cancellation_request PRIMARY KEY (id),
    CONSTRAINT uq_certificate_cancellation_request_active_dni UNIQUE (active_dni_guard),
    CONSTRAINT chk_request_dni_hash
        CHECK (REGEXP_LIKE(dni_lookup_hash, '^[0-9a-f]{64}$', 'c')),
    CONSTRAINT chk_request_dni_ciphertext CHECK (OCTET_LENGTH(dni_ciphertext) > 0),
    CONSTRAINT chk_request_dni_last_four
        CHECK (REGEXP_LIKE(dni_last_four, '^[0-9]{4}$', 'c')),
    CONSTRAINT chk_request_status CHECK (request_status IN (
        'STARTED', 'CHECKING_ELIGIBILITY', 'NOT_ELIGIBLE', 'ELIGIBLE',
        'PENDING_IDENTITY_VERIFICATION', 'IDENTITY_VERIFIED', 'REASON_REGISTERED',
        'PENDING_CONFIRMATION', 'CONFIRMED', 'REVOCATION_IN_PROGRESS', 'COMPLETED',
        'FAILED', 'OUTCOME_UNKNOWN', 'RECEIPT_AVAILABLE', 'EXPIRED', 'ABANDONED'
    )),
    CONSTRAINT chk_request_lifecycle
        CHECK (lifecycle_status IN ('ACTIVE', 'FINALIZED', 'ABANDONED', 'EXPIRED')),
    CONSTRAINT chk_request_eligibility CHECK (eligibility_result IN (
        'NOT_CHECKED', 'ELIGIBLE', 'NOT_ELIGIBLE', 'UNAVAILABLE', 'INCONCLUSIVE'
    )),
    CONSTRAINT chk_request_reason CHECK (
        reason_code IS NULL OR reason_code IN (
            'THEFT', 'LOSS', 'DEVICE_OR_NUMBER_CHANGE', 'SUSPECTED_UNAUTHORIZED_USE', 'OTHER'
        )
    ),
    CONSTRAINT chk_request_other_reason CHECK (
        (reason_code = 'OTHER' AND other_reason_ciphertext IS NOT NULL
            AND OCTET_LENGTH(other_reason_ciphertext) > 0 AND other_reason_key_version IS NOT NULL)
        OR (reason_code IS NULL AND other_reason_ciphertext IS NULL AND other_reason_key_version IS NULL)
        OR (reason_code <> 'OTHER' AND other_reason_ciphertext IS NULL AND other_reason_key_version IS NULL)
    ),
    CONSTRAINT chk_request_confirmation CHECK (
        (consent_text_version IS NULL AND confirmed_at IS NULL)
        OR (reason_code IS NOT NULL AND consent_text_version IS NOT NULL AND confirmed_at IS NOT NULL)
    ),
    CONSTRAINT chk_request_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_request_recoverability
        CHECK (recoverable_until IS NULL OR recoverable_until >= created_at),
    CONSTRAINT chk_request_confirmation_time
        CHECK (confirmed_at IS NULL OR confirmed_at >= created_at),
    INDEX idx_request_dni_history (dni_lookup_hash, created_at DESC),
    INDEX idx_request_expiration (lifecycle_status, expires_at),
    INDEX idx_request_recovery (dni_lookup_hash, lifecycle_status, recoverable_until)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE certificate_eligibility_check (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    check_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    normalized_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL,
    external_reference VARCHAR(128) NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    responded_at TIMESTAMP(6) NULL,
    technical_error_code VARCHAR(64) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_certificate_eligibility_check PRIMARY KEY (id),
    CONSTRAINT fk_eligibility_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_eligibility_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT chk_eligibility_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_eligibility_status
        CHECK (check_status IN ('CREATED', 'SUBMITTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_eligibility_result CHECK (
        normalized_result IS NULL OR normalized_result IN (
            'ELIGIBLE', 'NOT_ELIGIBLE', 'UNAVAILABLE', 'INCONCLUSIVE'
        )
    ),
    CONSTRAINT chk_eligibility_response_time
        CHECK (responded_at IS NULL OR responded_at >= requested_at),
    INDEX idx_eligibility_latest (request_id, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE identity_verification (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    provider VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    verification_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_reference VARCHAR(128) NULL,
    verified_identity_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    dni_match_result VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    error_or_cancellation_code VARCHAR(64) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_identity_verification PRIMARY KEY (id),
    CONSTRAINT fk_identity_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_identity_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT chk_identity_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_identity_status CHECK (verification_status IN (
        'STARTED', 'VERIFIED', 'REJECTED', 'CANCELLED', 'IDENTITY_MISMATCH', 'ERROR'
    )),
    CONSTRAINT chk_identity_match
        CHECK (dni_match_result IN ('NOT_EVALUATED', 'MATCH', 'MISMATCH', 'INCONCLUSIVE')),
    CONSTRAINT chk_identity_hash CHECK (
        verified_identity_hash IS NULL
        OR REGEXP_LIKE(verified_identity_hash, '^[0-9a-f]{64}$', 'c')
    ),
    CONSTRAINT chk_identity_completion
        CHECK (completed_at IS NULL OR completed_at >= started_at),
    INDEX idx_identity_latest_valid (request_id, verification_status, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cancellation_request_session (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    session_reference_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    token_family_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    last_used_at TIMESTAMP(6) NULL,
    invalidated_at TIMESTAMP(6) NULL,
    invalidation_reason VARCHAR(64) NULL,
    client_reference_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_request_session PRIMARY KEY (id),
    CONSTRAINT fk_session_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_session_reference UNIQUE (session_reference_hash),
    CONSTRAINT chk_session_reference
        CHECK (REGEXP_LIKE(session_reference_hash, '^[0-9a-f]{64}$', 'c')),
    CONSTRAINT chk_session_client_reference CHECK (
        client_reference_hash IS NULL
        OR REGEXP_LIKE(client_reference_hash, '^[0-9a-f]{64}$', 'c')
    ),
    CONSTRAINT chk_session_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_session_last_use
        CHECK (last_used_at IS NULL OR last_used_at >= created_at),
    CONSTRAINT chk_session_invalidation CHECK (
        (invalidated_at IS NULL AND invalidation_reason IS NULL)
        OR (invalidated_at IS NOT NULL AND invalidated_at >= created_at AND invalidation_reason IS NOT NULL)
    ),
    INDEX idx_session_active (request_id, invalidated_at, expires_at)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE revocation_operation (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    idempotency_key BINARY(16) NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    operation_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_reference VARCHAR(128) NULL,
    prepared_at TIMESTAMP(6) NOT NULL,
    submitted_at TIMESTAMP(6) NULL,
    responded_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    normalized_result VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    technical_error_code VARCHAR(64) NULL,
    next_status_check_at TIMESTAMP(6) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL,
    open_request_guard BINARY(16)
        GENERATED ALWAYS AS (
            CASE WHEN operation_status IN ('PREPARED', 'SUBMITTED', 'OUTCOME_UNKNOWN')
                THEN request_id ELSE NULL END
        ) STORED,
    CONSTRAINT pk_revocation_operation PRIMARY KEY (id),
    CONSTRAINT fk_revocation_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_revocation_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_revocation_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT uq_revocation_open_request UNIQUE (open_request_guard),
    CONSTRAINT chk_revocation_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_revocation_status CHECK (operation_status IN (
        'PREPARED', 'SUBMITTED', 'SUCCEEDED', 'FAILED', 'OUTCOME_UNKNOWN'
    )),
    CONSTRAINT chk_revocation_submission
        CHECK (submitted_at IS NULL OR submitted_at >= prepared_at),
    CONSTRAINT chk_revocation_response
        CHECK (responded_at IS NULL OR responded_at >= prepared_at),
    CONSTRAINT chk_revocation_completion
        CHECK (completed_at IS NULL OR completed_at >= prepared_at),
    INDEX idx_revocation_current (request_id, operation_status, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cancellation_receipt (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    revocation_operation_id BINARY(16) NOT NULL,
    receipt_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    generation_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    storage_reference VARCHAR(256) NULL,
    document_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    template_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    generated_at TIMESTAMP(6) NULL,
    available_at TIMESTAMP(6) NULL,
    technical_error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_receipt PRIMARY KEY (id),
    CONSTRAINT fk_receipt_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT fk_receipt_revocation FOREIGN KEY (revocation_operation_id)
        REFERENCES revocation_operation (id),
    CONSTRAINT uq_receipt_code UNIQUE (receipt_code),
    CONSTRAINT chk_receipt_status
        CHECK (generation_status IN ('PENDING', 'GENERATING', 'AVAILABLE', 'FAILED')),
    CONSTRAINT chk_receipt_document_hash CHECK (
        document_hash IS NULL OR REGEXP_LIKE(document_hash, '^[0-9a-f]{64}$', 'c')
    ),
    CONSTRAINT chk_receipt_availability CHECK (
        available_at IS NULL OR (generated_at IS NOT NULL AND available_at >= generated_at)
    ),
    INDEX idx_receipt_available (request_id, generation_status, available_at DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cancellation_audit_event (
    id BINARY(16) NOT NULL,
    request_id BINARY(16) NOT NULL,
    event_type VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    previous_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
    new_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
    result VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_reference VARCHAR(128) NULL,
    technical_code VARCHAR(64) NULL,
    technical_detail VARCHAR(255) NULL,
    event_origin VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_audit_event PRIMARY KEY (id),
    CONSTRAINT fk_audit_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    INDEX idx_audit_history (request_id, occurred_at, id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
