export interface paths {
    "/actuator/health": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Comprueba la salud operativa agregada
         * @description Informa si la aplicación y sus dependencias configuradas están operativas sin exponer detalles internos.
         */
        get: operations["getActuatorHealth"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/identity-verifications": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Inicia la autenticación con ID Perú
         * @description Valida la continuidad temporal, crea state y PKCE de un solo uso y devuelve la URL construida por el backend.
         */
        post: operations["startIdentityVerification"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/identity-verifications/current": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Consulta el estado de autenticación actual
         * @description Resuelve el intento desde la cookie HttpOnly, valida la autorización y consume el resultado de presentación del callback.
         */
        get: operations["getCurrentIdentityVerification"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/idperu/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Procesa el retorno GET de ID Perú
         * @description Recibe el retorno del navegador, valida el intento y siempre redirige con HTTP 303 a una ruta frontend fija.
         */
        get: operations["handleIdentityCallbackGet"];
        put?: never;
        /**
         * Procesa el retorno POST de ID Perú
         * @description Recibe el formulario del proveedor, aplica el mismo caso de uso y siempre redirige con HTTP 303 a una ruta frontend fija.
         */
        post: operations["handleIdentityCallbackPost"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Inicia una solicitud y consulta si existen credenciales disponibles
         * @description Valida el DNI y Google reCAPTCHA v2 Checkbox antes de crear una solicitud y consultar únicamente si existe al menos una credencial disponible para revocar. No obtiene una lista, cantidad, número de orden, fecha de creación ni UUID; tampoco reabre solicitudes anteriores ni expone el DNI completo o la evidencia anti-bot.
         */
        post: operations["initiateRevocationRequest"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/confirmation": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Persiste la decisión completa y confirma la solicitud
         * @description Guarda la decisión, ejecuta una revocación idempotente y genera la constancia cuando el resultado es exitoso.
         */
        post: operations["confirmCurrentRevocation"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/digital-credentials": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Obtiene el listado de credenciales de la solicitud autenticada
         * @description Consulta el segundo servicio solo en la primera carga y luego devuelve la instantánea persistida.
         */
        get: operations["getCurrentRequestDigitalCredentials"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/execution": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Reanuda idempotentemente una revocación ya confirmada */
        post: operations["resumeCurrentRevocationExecution"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/outcome": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Consulta el resultado y la constancia de la solicitud actual */
        get: operations["getCurrentRevocationOutcome"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/receipt": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Descarga la constancia de la sesión autenticada */
        get: operations["downloadCurrentRevocationReceipt"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/receipt/retry": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Reintenta únicamente la generación de la constancia */
        post: operations["retryCurrentRevocationReceipt"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/revocation-requests/current/review": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Recupera el resumen de una solicitud confirmada
         * @description Disponible únicamente después de confirmar; no recupera borradores.
         */
        get: operations["getConfirmedRevocationReview"];
        put?: never;
        /** Valida el borrador y prepara el resumen sin persistirlo */
        post: operations["previewCurrentRevocation"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/session/current": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Consulta la sesión y el paso actualmente autorizado */
        get: operations["getCurrentFlowSession"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/session/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Cierra la sesión y abandona la operación activa reversible */
        post: operations["logoutFlowSession"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/session/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Rota el refresh token y actualiza el access token */
        post: operations["refreshFlowSession"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/system/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Comprueba la disponibilidad del backend y MySQL
         * @description Ejecuta una comprobación ligera y actual de MySQL. No devuelve credenciales, coordenadas de conexión ni detalles internos.
         */
        get: operations["getSystemStatus"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description Estado agregado y seguro publicado por Spring Boot Actuator. */
        ActuatorHealthResponse: {
            /**
             * @description Estado agregado.
             * @example UP
             */
            status: string;
        };
        /** @description Formato común y seguro de los errores controlados de la API. */
        ApiError: {
            /**
             * @description Código público y estable del error.
             * @example VALIDATION_ERROR
             */
            code: string;
            /**
             * @description Identificador de correlación para soporte y trazabilidad.
             * @example 7a5f3f75-3bd2-4c47-90fc-6cfc79f1ec2d
             */
            correlationId: string;
            /**
             * @description Mensaje comprensible sin detalles internos.
             * @example La solicitud contiene datos inválidos.
             */
            message: string;
            /**
             * @description Ruta de la solicitud que produjo el error.
             * @example /api/v1/revocation-requests
             */
            path: string;
            /**
             * Format: date-time
             * @description Fecha y hora UTC del error.
             * @example 2026-07-20T18:30:00Z
             */
            timestamp: string;
        };
        CurrentIdentityResponse: {
            /**
             * @description Resultado efímero del último callback, consumido una sola vez para presentación.
             * @enum {string|null}
             */
            callbackOutcome?: "CANCELLED" | "REJECTED" | "IDENTITY_MISMATCH" | "EXPIRED" | "TIMEOUT" | "UNAVAILABLE" | "ERROR" | null;
            /** @description Indica si la autorización temporal permite continuar. */
            canContinue: boolean;
            /**
             * @description Siguiente paso autorizado.
             * @enum {string}
             */
            nextStep: "IDENTITY_VERIFICATION" | "DIGITAL_CREDENTIAL_SELECTION";
            /**
             * @description Estado normalizado del intento de identidad.
             * @enum {string}
             */
            status: "STARTED" | "VERIFIED" | "REJECTED" | "CANCELLED" | "EXPIRED" | "IDENTITY_MISMATCH" | "ERROR";
        };
        /** @description Contexto seguro de la operación activa y el siguiente paso autorizado. */
        CurrentSession: {
            /** @description DNI completo mostrado únicamente dentro de la sesión autenticada. */
            dni: string;
            /** @enum {string} */
            nextStep: "IDENTITY_VERIFICATION" | "DIGITAL_CREDENTIAL_SELECTION" | "CONFIRMATION" | "RECEIPT";
            /**
             * Format: int64
             * @description Identificador técnico de solicitud.
             */
            requestId: number;
            /** @description Estado controlado de la solicitud. */
            requestStatus: string;
            /**
             * Format: int64
             * @description Identificador técnico de sesión.
             */
            sessionId: number;
            /** @enum {string} */
            sessionStatus: "PENDING_IDENTITY" | "IDENTITY_VERIFIED";
        };
        DigitalCredential: {
            /** Format: date-time */
            emissionCreatedAt: string;
            /** Format: int32 */
            statusListIndex: number;
        };
        /** @description Credencial digital obtenida después de autenticar al ciudadano. */
        DigitalCredentialItem: {
            /** Format: uuid */
            digitalCredentialUuid: string;
            /** Format: date-time */
            emissionCreatedAt: string;
            /** Format: date-time */
            revokedAt: string | null;
            selected: boolean;
            /** @enum {string} */
            status: "ACTIVE" | "REVOKED";
            /** Format: int32 */
            statusListIndex: number;
        };
        /** @description Listado persistido de credenciales digitales de la solicitud autenticada. */
        DigitalCredentialListResponse: {
            canContinue: boolean;
            digitalCredentials: components["schemas"]["DigitalCredentialItem"][];
            requestStatus: string;
        };
        IdentityStartResponse: {
            /**
             * Format: uri
             * @description URL de autorización construida por el backend.
             */
            authorizationUrl: string;
        };
        Processing: {
            /** @enum {string} */
            phase: "SUBMITTING" | "PROPAGATING" | "GENERATING";
            /** Format: date-time */
            readyAt?: string;
            /** Format: date-time */
            serverTime: string;
            /** Format: date-time */
            startedAt: string;
        };
        Receipt: {
            /** Format: date-time */
            availableAt?: string;
            code: string;
            downloadAvailable: boolean;
            /** @enum {string} */
            status: "PENDING" | "GENERATING" | "AVAILABLE" | "FAILED";
        };
        /** @description Decisión completa y consentimiento explícito presentados en el paso 4. */
        RevocationConfirmationRequest: {
            /** @description Aceptación expresa del texto mostrado; debe enviarse con valor true. */
            consentAccepted: boolean;
            /** @description Versión exacta del consentimiento mostrado por el backend. */
            consentVersion: string;
            /** Format: uuid */
            digitalCredentialUuid: string;
            /** @description Descripción requerida únicamente para OTHER. */
            otherReason?: string;
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
        };
        /** @description Resultado ciudadano de la revocacion sin exponer identificadores sensibles. */
        RevocationExecutionResponse: {
            /** Format: date-time */
            completedAt?: string;
            /** Format: date-time */
            confirmedAt?: string;
            digitalCredential: components["schemas"]["DigitalCredential"];
            /**
             * @description Primer nombre verificado por ID Perú; ausente solo en evidencia histórica.
             * @example ANA
             */
            firstName?: string;
            /** @example ******91 */
            maskedDni: string;
            otherReason?: string;
            processing?: components["schemas"]["Processing"];
            reasonLabel: string;
            receipt?: components["schemas"]["Receipt"];
            /**
             * @description Estado actual de la solicitud ciudadana de revocación.
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_DIGITAL_CREDENTIALS_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST" | "CHECKING_DIGITAL_CREDENTIAL_LIST" | "DIGITAL_CREDENTIALS_AVAILABLE" | "DIGITAL_CREDENTIALS_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
            /** @enum {string} */
            state: "PROCESSING" | "SUCCEEDED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_FAILED";
        };
        /** @description Resultado normalizado del inicio de una nueva solicitud de revocación. */
        RevocationRequestResponse: {
            /**
             * @description Resultado normalizado de la consulta inicial de existencia. No representa una lista detallada.
             * @example AVAILABLE
             * @enum {string}
             */
            availabilityResult: "AVAILABLE" | "NOT_AVAILABLE" | "INCONCLUSIVE" | "UNAVAILABLE" | "ERROR";
            /**
             * @description Indica si el backend autoriza continuar al siguiente paso.
             * @example true
             */
            canContinue: boolean;
            /**
             * @description DNI enmascarado para presentación segura.
             * @example ******01
             */
            maskedDni: string;
            /**
             * @description Siguiente paso autorizado. Es nulo cuando no se permite continuar.
             * @example IDENTITY_VERIFICATION
             * @enum {string|null}
             */
            nextStep: "IDENTITY_VERIFICATION" | null;
            /**
             * Format: int64
             * @description Identificador interno de la solicitud. No funciona como credencial ni autorización.
             * @example 125
             */
            requestId: number;
            /**
             * @description Estado actual persistido de la solicitud.
             * @example PENDING_IDENTITY_VERIFICATION
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_DIGITAL_CREDENTIALS_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST" | "CHECKING_DIGITAL_CREDENTIAL_LIST" | "DIGITAL_CREDENTIALS_AVAILABLE" | "DIGITAL_CREDENTIALS_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
        };
        /** @description Borrador efímero que se valida para presentar el paso 4 sin persistirlo. */
        RevocationReviewRequest: {
            /** Format: uuid */
            digitalCredentialUuid: string;
            /** @description Descripción requerida únicamente para OTHER. */
            otherReason?: string;
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
        };
        /** @description Resumen autoritativo y minimizado de la revocación. */
        RevocationReviewResponse: {
            confirmed: boolean;
            /**
             * Format: date-time
             * @description Fecha UTC persistida; ausente antes de confirmar.
             */
            confirmedAt?: string;
            consentText: string;
            consentVersion: string;
            consequences: string[];
            digitalCredential: components["schemas"]["SelectedDigitalCredential"];
            /**
             * @description Primer nombre verificado por ID Perú; ausente solo en evidencia histórica.
             * @example ANA
             */
            firstName?: string;
            /** @example ******91 */
            maskedDni: string;
            /** @description Descripción validada para el motivo OTHER. */
            otherReason?: string;
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
            reasonLabel: string;
            /**
             * @description Estado actual de la solicitud ciudadana de revocación.
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_DIGITAL_CREDENTIALS_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST" | "CHECKING_DIGITAL_CREDENTIAL_LIST" | "DIGITAL_CREDENTIALS_AVAILABLE" | "DIGITAL_CREDENTIALS_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
        };
        /** @description Credencial identificado por datos visibles, sin exponer su UUID. */
        SelectedDigitalCredential: {
            /** Format: date-time */
            emissionCreatedAt: string;
            /** Format: int32 */
            statusListIndex: number;
        };
        /** @description Datos requeridos para iniciar una nueva solicitud y consultar si existen credenciales disponibles. */
        StartRevocationRequest: {
            /** @description Número de DNI del ciudadano. Debe contener exactamente ocho dígitos ASCII. Por privacidad, la documentación no incluye un DNI completo de ejemplo. */
            dni: string;
            /** @description Evidencia efímera de Google reCAPTCHA v2 Checkbox. No se almacena ni se devuelve. */
            recaptchaToken: string;
        };
        /** @description Estado técnico del backend y su conexión con MySQL */
        SystemStatusResponse: {
            /**
             * @description Disponibilidad de la conexión comprobada con MySQL.
             * @example UP
             */
            database: string;
            /**
             * @description Estado agregado del backend.
             * @example UP
             */
            status: string;
            /**
             * Format: date-time
             * @description Fecha y hora UTC de la comprobación.
             * @example 2026-07-20T18:30:00Z
             */
            timestamp: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    getActuatorHealth: {
        parameters: {
            query?: never;
            header?: {
                /** @description Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos. */
                "X-Correlation-ID"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Aplicación operativa */
            200: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ActuatorHealthResponse"];
                };
            };
            /** @description Aplicación o dependencia no disponible */
            503: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ActuatorHealthResponse"];
                };
            };
        };
    };
    startIdentityVerification: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description URL de autorización preparada */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["IdentityStartResponse"];
                };
            };
            /** @description Continuidad ausente o inválida */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description La verificación no puede iniciarse en el estado actual */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Integración no disponible */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
    getCurrentIdentityVerification: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CurrentIdentityResponse"];
                };
            };
        };
    };
    handleIdentityCallbackGet: {
        parameters: {
            query?: {
                code?: string;
                state?: string;
                session_state?: string;
                error?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Retorno procesado y redirección controlada */
            303: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    handleIdentityCallbackPost: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/x-www-form-urlencoded": {
                    code?: string;
                    error?: string;
                    session_state?: string;
                    state?: string;
                };
            };
        };
        responses: {
            /** @description Retorno procesado y redirección controlada */
            303: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    initiateRevocationRequest: {
        parameters: {
            query?: never;
            header?: {
                /** @description Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos. */
                "X-Correlation-ID"?: string;
            };
            path?: never;
            cookie?: never;
        };
        /** @description DNI y evidencia efímera reCAPTCHA que se validan en el backend antes de crear una solicitud. */
        requestBody: {
            content: {
                "application/json": components["schemas"]["StartRevocationRequest"];
            };
        };
        responses: {
            /** @description Resultado normalizado de existencia de credenciales */
            200: {
                headers: {
                    /** @description Identificador de correlación */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationRequestResponse"];
                };
            };
            /** @description DNI, JSON, cuerpo o evidencia reCAPTCHA inválida: VALIDATION_ERROR, RECAPTCHA_REQUIRED, RECAPTCHA_REJECTED o RECAPTCHA_EXPIRED_OR_DUPLICATE */
            400: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Conflicto controlado: AVAILABILITY_CHECK_IN_PROGRESS, REVOCATION_REQUEST_IN_PROGRESS o CONCURRENT_REQUEST */
            409: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Tipo de contenido no admitido */
            415: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Error interno controlado */
            500: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description RECAPTCHA_INVALID_RESPONSE o error controlado del proveedor de disponibilidad */
            502: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description RECAPTCHA_UNAVAILABLE o servicio de disponibilidad no disponible */
            503: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description RECAPTCHA_TIMEOUT o tiempo de espera agotado en disponibilidad */
            504: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
    confirmCurrentRevocation: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RevocationConfirmationRequest"];
            };
        };
        responses: {
            /** @description Solicitud confirmada o repetición idempotente */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationExecutionResponse"];
                };
            };
            /** @description Consentimiento ausente o formato inválido */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Sesión ausente o expirada */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Identidad o paso no permitido */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Versión o decisión incompatible */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Motivo o selección inválida */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Integración de revocación no disponible; la decisión no se persiste */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
    getCurrentRequestDigitalCredentials: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Listado persistido, incluido el escenario vacío */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["DigitalCredentialListResponse"];
                };
            };
            /** @description Sesión ausente o expirada */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Identidad no verificada o paso no permitido */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Consulta concurrente */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Respuesta externa inválida */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Servicio de listado no disponible */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Timeout del servicio de listado */
            504: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
    resumeCurrentRevocationExecution: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationExecutionResponse"];
                };
            };
        };
    };
    getCurrentRevocationOutcome: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationExecutionResponse"];
                };
            };
        };
    };
    downloadCurrentRevocationReceipt: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/pdf": string;
                };
            };
        };
    };
    retryCurrentRevocationReceipt: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationExecutionResponse"];
                };
            };
        };
    };
    getConfirmedRevocationReview: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationReviewResponse"];
                };
            };
        };
    };
    previewCurrentRevocation: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RevocationReviewRequest"];
            };
        };
        responses: {
            /** @description Resumen vigente sin persistencia del borrador */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RevocationReviewResponse"];
                };
            };
            /** @description Formato inválido */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Sesión ausente o expirada */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Identidad o paso no permitido */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Motivo o credencial inválido */
            422: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
    getCurrentFlowSession: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CurrentSession"];
                };
            };
        };
    };
    logoutFlowSession: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    refreshFlowSession: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getSystemStatus: {
        parameters: {
            query?: never;
            header?: {
                /** @description Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos. */
                "X-Correlation-ID"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Backend y base de datos disponibles */
            200: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SystemStatusResponse"];
                };
            };
            /** @description Error interno controlado */
            500: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description MySQL no está disponible */
            503: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
}
