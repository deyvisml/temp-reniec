-- La creación falla de forma segura si existen solicitudes con más de una fila seleccionada.
-- No se elige ni se elimina silenciosamente ningún certificado previamente registrado.
CREATE UNIQUE INDEX uq_request_certificate_single_selected
    ON cancellation_request_certificate ((CASE WHEN selected = TRUE THEN request_id ELSE NULL END));
