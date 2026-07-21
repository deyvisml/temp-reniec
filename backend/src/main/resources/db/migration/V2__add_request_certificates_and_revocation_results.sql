ALTER TABLE certificate_eligibility_check
    ADD CONSTRAINT uq_eligibility_request_identity UNIQUE (request_id, id);

ALTER TABLE revocation_operation
    ADD CONSTRAINT uq_revocation_request_identity UNIQUE (request_id, id);

CREATE TABLE cancellation_request_certificate (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    eligibility_check_id BIGINT UNSIGNED NOT NULL,
    order_number VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    emission_created_at TIMESTAMP(6) NOT NULL,
    certificate_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    availability_status VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    consulted_at TIMESTAMP(6) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    selected_at TIMESTAMP(6) NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_cancellation_request_certificate PRIMARY KEY (id),
    CONSTRAINT fk_request_certificate_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT fk_request_certificate_eligibility FOREIGN KEY (request_id, eligibility_check_id)
        REFERENCES certificate_eligibility_check (request_id, id),
    CONSTRAINT uq_request_certificate_uuid UNIQUE (request_id, certificate_uuid),
    CONSTRAINT uq_request_certificate_identity UNIQUE (request_id, id, certificate_uuid),
    CONSTRAINT chk_request_certificate_uuid CHECK (
        REGEXP_LIKE(
            certificate_uuid,
            '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
            'c'
        )
    ),
    CONSTRAINT chk_request_certificate_selection CHECK (
        (selected = FALSE AND selected_at IS NULL)
        OR (selected = TRUE AND selected_at IS NOT NULL)
    ),
    CONSTRAINT chk_request_certificate_consulted CHECK (
        consulted_at >= emission_created_at
    ),
    INDEX idx_request_certificate_source (eligibility_check_id),
    INDEX idx_request_certificate_list (request_id, emission_created_at DESC, id),
    INDEX idx_request_certificate_selection (request_id, selected, availability_status)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE certificate_revocation_result (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    revocation_operation_id BIGINT UNSIGNED NOT NULL,
    request_certificate_id BIGINT UNSIGNED NOT NULL,
    submitted_uuid CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result_status VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result_code VARCHAR(64) NULL,
    result_message VARCHAR(256) NULL,
    external_reference VARCHAR(128) NULL,
    processed_at TIMESTAMP(6) NULL,
    correlation_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_certificate_revocation_result PRIMARY KEY (id),
    CONSTRAINT fk_certificate_result_request FOREIGN KEY (request_id)
        REFERENCES certificate_cancellation_request (id),
    CONSTRAINT fk_certificate_result_operation FOREIGN KEY (request_id, revocation_operation_id)
        REFERENCES revocation_operation (request_id, id),
    CONSTRAINT fk_certificate_result_certificate FOREIGN KEY (
        request_id,
        request_certificate_id,
        submitted_uuid
    ) REFERENCES cancellation_request_certificate (request_id, id, certificate_uuid),
    CONSTRAINT uq_operation_certificate_result UNIQUE (
        revocation_operation_id,
        request_certificate_id
    ),
    CONSTRAINT chk_certificate_result_processing CHECK (
        (result_status = 'PENDING' AND processed_at IS NULL)
        OR (result_status <> 'PENDING' AND processed_at IS NOT NULL)
    ),
    INDEX idx_certificate_result_operation (
        revocation_operation_id,
        result_status,
        id
    ),
    INDEX idx_certificate_result_certificate (
        request_certificate_id,
        created_at DESC
    )
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
