CREATE TABLE certificate_cancellation_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dni CHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    eligibility_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason_code VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    other_reason VARCHAR(300) NULL,
    confirmed_at TIMESTAMP(6) NULL,
    final_outcome VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_certificate_cancellation_request PRIMARY KEY (id),
    CONSTRAINT chk_request_dni CHECK (REGEXP_LIKE(dni, '^[0-9]{8}$', 'c')),
    CONSTRAINT chk_request_confirmation_time
        CHECK (confirmed_at IS NULL OR confirmed_at >= created_at),
    INDEX idx_request_dni_status_created (dni, request_status, created_at DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE certificate_eligibility_check (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    check_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    normalized_result VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL,
    external_reference VARCHAR(128) NULL,
    requested_at TIMESTAMP(6) NOT NULL,
    responded_at TIMESTAMP(6) NULL,
    error_code VARCHAR(64) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_certificate_eligibility_check PRIMARY KEY (id),
    CONSTRAINT fk_eligibility_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_eligibility_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT chk_eligibility_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_eligibility_response_time
        CHECK (responded_at IS NULL OR responded_at >= requested_at),
    INDEX idx_eligibility_latest (request_id, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE identity_verification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    provider VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    verification_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_reference VARCHAR(128) NULL,
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
    CONSTRAINT chk_identity_completion
        CHECK (completed_at IS NULL OR completed_at >= started_at),
    INDEX idx_identity_latest_valid (request_id, verification_status, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE revocation_operation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    attempt_number INT UNSIGNED NOT NULL,
    operation_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_reference VARCHAR(128) NULL,
    prepared_at TIMESTAMP(6) NOT NULL,
    submitted_at TIMESTAMP(6) NULL,
    responded_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    normalized_result VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL,
    error_code VARCHAR(64) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_revocation_operation PRIMARY KEY (id),
    CONSTRAINT fk_revocation_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT uq_revocation_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_revocation_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT chk_revocation_attempt CHECK (attempt_number > 0),
    CONSTRAINT chk_revocation_submission
        CHECK (submitted_at IS NULL OR submitted_at >= prepared_at),
    CONSTRAINT chk_revocation_response
        CHECK (responded_at IS NULL OR responded_at >= prepared_at),
    CONSTRAINT chk_revocation_completion
        CHECK (completed_at IS NULL OR completed_at >= prepared_at),
    INDEX idx_revocation_current (request_id, operation_status, attempt_number DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cancellation_receipt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    revocation_operation_id BIGINT UNSIGNED NOT NULL,
    receipt_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    generation_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    storage_reference VARCHAR(256) NULL,
    generated_at TIMESTAMP(6) NULL,
    available_at TIMESTAMP(6) NULL,
    error_code VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_receipt PRIMARY KEY (id),
    CONSTRAINT fk_receipt_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT fk_receipt_revocation FOREIGN KEY (revocation_operation_id)
        REFERENCES revocation_operation (id),
    CONSTRAINT uq_receipt_code UNIQUE (receipt_code),
    CONSTRAINT chk_receipt_availability CHECK (
        available_at IS NULL OR (generated_at IS NOT NULL AND available_at >= generated_at)
    ),
    INDEX idx_receipt_available (request_id, generation_status, available_at DESC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cancellation_audit_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    event_type VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    previous_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
    new_status VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
    result VARCHAR(64) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_origin VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_audit_event PRIMARY KEY (id),
    CONSTRAINT fk_audit_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    INDEX idx_audit_history (request_id, occurred_at, id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
