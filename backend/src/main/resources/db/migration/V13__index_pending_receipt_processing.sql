CREATE INDEX idx_revocation_receipt_processing
    ON revocation_receipt (generation_status, updated_at, request_id);
