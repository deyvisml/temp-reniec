ALTER TABLE certificate_cancellation_request
    ADD COLUMN consent_version VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'Versión del texto de consentimiento aceptado por el ciudadano'
        AFTER confirmed_at;
