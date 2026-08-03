package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Siguiente paso que el backend permite ejecutar.")
public enum RevocationRequestNextStep { IDENTITY_VERIFICATION }
