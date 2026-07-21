DROP TABLE certificate_revocation_result;

ALTER TABLE cancellation_request_certificate
    DROP INDEX uq_request_certificate_identity;

ALTER TABLE revocation_operation
    DROP INDEX uq_revocation_request_identity;
