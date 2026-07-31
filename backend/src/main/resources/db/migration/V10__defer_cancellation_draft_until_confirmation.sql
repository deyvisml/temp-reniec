UPDATE cancellation_request_certificate certificate
JOIN certificate_cancellation_request request
  ON request.id = certificate.request_id
SET certificate.selected = FALSE,
    certificate.selected_at = NULL
WHERE request.confirmed_at IS NULL
  AND (certificate.selected = TRUE OR certificate.selected_at IS NOT NULL);

UPDATE certificate_cancellation_request
SET reason_code = NULL,
    other_reason = NULL
WHERE confirmed_at IS NULL
  AND (reason_code IS NOT NULL OR other_reason IS NOT NULL);

UPDATE certificate_cancellation_request
SET request_status = 'CERTIFICATES_AVAILABLE'
WHERE confirmed_at IS NULL
  AND request_status IN (
    'CERTIFICATES_SELECTED',
    'REASON_REGISTERED',
    'PENDING_CONFIRMATION'
  );
