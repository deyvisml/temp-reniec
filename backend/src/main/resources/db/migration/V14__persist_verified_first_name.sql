ALTER TABLE identity_verification
    ADD COLUMN verified_first_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL
        COMMENT 'Primer nombre verificado por ID Perú; nulo solo para intentos no exitosos o registros históricos'
        AFTER verified_subject_hash;

UPDATE digital_credential_revocation_request
SET request_status = 'PENDING_IDENTITY_VERIFICATION'
WHERE confirmed_at IS NULL
  AND request_status IN (
      'IDENTITY_VERIFIED',
      'AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST',
      'CHECKING_DIGITAL_CREDENTIAL_LIST',
      'DIGITAL_CREDENTIALS_AVAILABLE',
      'DIGITAL_CREDENTIALS_SELECTED',
      'REASON_REGISTERED',
      'PENDING_CONFIRMATION'
  );

UPDATE revocation_flow_session session
JOIN digital_credential_revocation_request request ON request.id = session.request_id
SET session.session_status = 'PENDING_IDENTITY',
    session.updated_at = CURRENT_TIMESTAMP(6)
WHERE request.request_status = 'PENDING_IDENTITY_VERIFICATION'
  AND session.session_status = 'IDENTITY_VERIFIED';
