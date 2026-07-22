package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Siguiente paso que el backend permite ejecutar.")
public enum CancellationRequestNextStep { IDENTITY_VERIFICATION }
