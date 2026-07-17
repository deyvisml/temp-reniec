# Cambio sustituido

`add-mysql-persistence-foundation` fue implementado como base temporal y su modelo de `cancellation_process` y `cancellation_session` quedó sustituido por `redesign-cancellation-request-data-model`.

Su delta `mysql-persistence-foundation` no debe sincronizarse ni archivarse como contrato definitivo. El cambio autoritativo para persistencia es el modelo centrado en `certificate_cancellation_request`.
