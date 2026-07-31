export interface paths {
    "/api/v1/session/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Rota el refresh token y renueva el access token */
        post: operations["refreshFlowSession"];
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
    "/api/v1/cancellation-requests": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Inicia una solicitud y consulta si existen certificados disponibles
         * @description Valida el DNI y Google reCAPTCHA v2 Checkbox antes de crear una solicitud y consultar únicamente si existe al menos un certificado disponible para cancelar. No obtiene una lista, cantidad, número de orden, fecha de creación ni UUID; tampoco reabre solicitudes anteriores ni expone el DNI completo o la evidencia anti-bot.
         */
        post: operations["initiateCancellationRequest"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/review": {
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
        get: operations["getConfirmedCancellationReview"];
        put?: never;
        /** Valida el borrador y prepara el resumen sin persistirlo */
        post: operations["previewCurrentCancellation"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/receipt/retry": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Reintenta únicamente la generación de la constancia */
        post: operations["retryCurrentCancellationReceipt"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/execution": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Reanuda idempotentemente una revocación ya confirmada */
        post: operations["resumeCurrentCancellationExecution"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/confirmation": {
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
        post: operations["confirmCurrentCancellation"];
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
    "/api/v1/cancellation-requests/current/receipt": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Descarga la constancia de la sesión autenticada */
        get: operations["downloadCurrentCancellationReceipt"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/outcome": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Consulta el resultado y la constancia de la solicitud actual */
        get: operations["getCurrentCancellationOutcome"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cancellation-requests/current/certificates": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Obtiene el listado de certificados de la solicitud autenticada
         * @description Consulta el segundo servicio solo en la primera carga y luego devuelve la instantánea persistida.
         */
        get: operations["getCurrentRequestCertificates"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
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
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description Formato común y seguro de los errores controlados de la API. */
        ApiError: {
            /**
             * @description Código público y estable del error.
             * @example VALIDATION_ERROR
             */
            code: string;
            /**
             * @description Mensaje comprensible sin detalles internos.
             * @example La solicitud contiene datos inválidos.
             */
            message: string;
            /**
             * Format: date-time
             * @description Fecha y hora UTC del error.
             * @example 2026-07-20T18:30:00Z
             */
            timestamp: string;
            /**
             * @description Ruta de la solicitud que produjo el error.
             * @example /api/v1/cancellation-requests
             */
            path: string;
            /**
             * @description Identificador de correlación para soporte y trazabilidad.
             * @example 7a5f3f75-3bd2-4c47-90fc-6cfc79f1ec2d
             */
            correlationId: string;
        };
        IdentityStartResponse: {
            /**
             * Format: uri
             * @description URL de autorización construida por el backend.
             */
            authorizationUrl: string;
        };
        /** @description Datos requeridos para iniciar una nueva solicitud y consultar si existen certificados disponibles. */
        StartCancellationRequest: {
            /** @description Número de DNI del ciudadano. Debe contener exactamente ocho dígitos ASCII. Por privacidad, la documentación no incluye un DNI completo de ejemplo. */
            dni: string;
            /** @description Evidencia efímera de Google reCAPTCHA v2 Checkbox. No se almacena ni se devuelve. */
            recaptchaToken: string;
        };
        /** @description Resultado normalizado del inicio de una nueva solicitud de cancelación. */
        CancellationRequestResponse: {
            /**
             * Format: int64
             * @description Identificador interno de la solicitud. No funciona como credencial ni autorización.
             * @example 125
             */
            requestId: number;
            /**
             * @description DNI enmascarado para presentación segura.
             * @example ******01
             */
            maskedDni: string;
            /**
             * @description Estado actual persistido de la solicitud.
             * @example PENDING_IDENTITY_VERIFICATION
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_CERTIFICATES_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_CERTIFICATE_LIST" | "CHECKING_CERTIFICATE_LIST" | "CERTIFICATES_AVAILABLE" | "CERTIFICATES_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
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
             * @description Siguiente paso autorizado. Es nulo cuando no se permite continuar.
             * @example IDENTITY_VERIFICATION
             * @enum {string|null}
             */
            nextStep: "IDENTITY_VERIFICATION" | null;
        };
        /** @description Borrador efímero que se valida para presentar el paso 4 sin persistirlo. */
        CancellationReviewRequest: {
            /** Format: uuid */
            certificateUuid: string;
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
            /** @description Descripción requerida únicamente para OTHER. */
            otherReason?: string;
        };
        /** @description Resumen autoritativo y minimizado de la cancelación. */
        CancellationReviewResponse: {
            /**
             * @description Estado actual de la solicitud ciudadana de cancelación.
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_CERTIFICATES_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_CERTIFICATE_LIST" | "CHECKING_CERTIFICATE_LIST" | "CERTIFICATES_AVAILABLE" | "CERTIFICATES_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
            /** @example ******91 */
            maskedDni: string;
            certificate: components["schemas"]["SelectedCertificate"];
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
            reasonLabel: string;
            /** @description Descripción validada para el motivo OTHER. */
            otherReason?: string;
            consequences: string[];
            consentText: string;
            consentVersion: string;
            /**
             * Format: date-time
             * @description Fecha UTC persistida; ausente antes de confirmar.
             */
            confirmedAt?: string;
            confirmed: boolean;
        };
        /** @description Certificado identificado por datos visibles, sin exponer su UUID. */
        SelectedCertificate: {
            orderNumber: string;
            /** Format: date-time */
            emissionCreatedAt: string;
        };
        /** @description Resultado ciudadano de la cancelacion sin exponer identificadores sensibles. */
        CancellationExecutionResponse: {
            /** @enum {string} */
            state: "PROCESSING" | "SUCCEEDED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_FAILED";
            /**
             * @description Estado actual de la solicitud ciudadana de cancelación.
             * @enum {string}
             */
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_CERTIFICATES_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_CERTIFICATE_LIST" | "CHECKING_CERTIFICATE_LIST" | "CERTIFICATES_AVAILABLE" | "CERTIFICATES_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
            /** @example ******91 */
            maskedDni: string;
            certificate: components["schemas"]["Certificate"];
            reasonLabel: string;
            otherReason?: string;
            /** Format: date-time */
            confirmedAt?: string;
            /** Format: date-time */
            completedAt?: string;
            receipt?: components["schemas"]["Receipt"];
        };
        Certificate: {
            orderNumber: string;
            /** Format: date-time */
            emissionCreatedAt: string;
        };
        Receipt: {
            code: string;
            /** @enum {string} */
            status: "PENDING" | "GENERATING" | "AVAILABLE" | "FAILED";
            /** Format: date-time */
            availableAt?: string;
            downloadAvailable: boolean;
        };
        /** @description Decisión completa y consentimiento explícito presentados en el paso 4. */
        CancellationConfirmationRequest: {
            /** Format: uuid */
            certificateUuid: string;
            /** @enum {string} */
            reasonCode: "THEFT" | "LOSS" | "DEVICE_OR_NUMBER_CHANGE" | "SUSPECTED_UNAUTHORIZED_USE" | "OTHER";
            /** @description Descripción requerida únicamente para OTHER. */
            otherReason?: string;
            /** @description Aceptación expresa del texto mostrado; debe enviarse con valor true. */
            consentAccepted: boolean;
            /** @description Versión exacta del consentimiento mostrado por el backend. */
            consentVersion: string;
        };
        /** @description Estado técnico del backend y su conexión con MySQL */
        SystemStatusResponse: {
            /**
             * @description Estado agregado del backend.
             * @example UP
             */
            status: string;
            /**
             * @description Disponibilidad de la conexión comprobada con MySQL.
             * @example UP
             */
            database: string;
            /**
             * Format: date-time
             * @description Fecha y hora UTC de la comprobación.
             * @example 2026-07-20T18:30:00Z
             */
            timestamp: string;
        };
        /** @description Contexto seguro de la operación activa y el siguiente paso autorizado. */
        CurrentSession: {
            /**
             * Format: int64
             * @description Identificador técnico de sesión.
             */
            sessionId: number;
            /**
             * Format: int64
             * @description Identificador técnico de solicitud.
             */
            requestId: number;
            /** @description DNI completo mostrado únicamente dentro de la sesión autenticada. */
            dni: string;
            /** @enum {string} */
            sessionStatus: "PENDING_IDENTITY" | "IDENTITY_VERIFIED";
            /** @description Estado controlado de la solicitud. */
            requestStatus: string;
            /** @enum {string} */
            nextStep: "IDENTITY_VERIFICATION" | "CERTIFICATE_SELECTION" | "CONFIRMATION" | "RECEIPT";
        };
        CurrentIdentityResponse: {
            /**
             * @description Estado normalizado del intento de identidad.
             * @enum {string}
             */
            status: "STARTED" | "VERIFIED" | "REJECTED" | "CANCELLED" | "EXPIRED" | "IDENTITY_MISMATCH" | "ERROR";
            /** @description Indica si la autorización temporal permite continuar. */
            canContinue: boolean;
            /**
             * @description Siguiente paso autorizado.
             * @enum {string}
             */
            nextStep: "IDENTITY_VERIFICATION" | "CERTIFICATE_SELECTION";
            /**
             * @description Resultado efímero del último callback, consumido una sola vez para presentación.
             * @enum {string|null}
             */
            callbackOutcome?: "CANCELLED" | "REJECTED" | "IDENTITY_MISMATCH" | "EXPIRED" | "TIMEOUT" | "UNAVAILABLE" | "ERROR" | null;
        };
        /** @description Certificado vigente obtenido después de autenticar al ciudadano. */
        CertificateItem: {
            orderNumber: string;
            /** Format: date-time */
            emissionCreatedAt: string;
            /** Format: uuid */
            certificateUuid: string;
            availabilityStatus: string;
            selected: boolean;
        };
        /** @description Listado persistido de certificados vigentes de la solicitud autenticada. */
        CertificateListResponse: {
            requestStatus: string;
            certificates: components["schemas"]["CertificateItem"][];
            canContinue: boolean;
        };
        /** @description Estado agregado y seguro publicado por Spring Boot Actuator. */
        ActuatorHealthResponse: {
            /**
             * @description Estado agregado.
             * @example UP
             */
            status: string;
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
                    state?: string;
                    session_state?: string;
                    error?: string;
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
    initiateCancellationRequest: {
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
                "application/json": components["schemas"]["StartCancellationRequest"];
            };
        };
        responses: {
            /** @description Resultado normalizado de existencia de certificados */
            200: {
                headers: {
                    /** @description Identificador de correlación */
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CancellationRequestResponse"];
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
            /** @description Conflicto controlado: AVAILABILITY_CHECK_IN_PROGRESS, CANCELLATION_REQUEST_IN_PROGRESS o CONCURRENT_REQUEST */
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
    getConfirmedCancellationReview: {
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
                    "application/json": components["schemas"]["CancellationReviewResponse"];
                };
            };
        };
    };
    previewCurrentCancellation: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CancellationReviewRequest"];
            };
        };
        responses: {
            /** @description Resumen vigente sin persistencia del borrador */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CancellationReviewResponse"];
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
            /** @description Motivo o certificado inválido */
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
    retryCurrentCancellationReceipt: {
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
                    "application/json": components["schemas"]["CancellationExecutionResponse"];
                };
            };
        };
    };
    resumeCurrentCancellationExecution: {
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
                    "application/json": components["schemas"]["CancellationExecutionResponse"];
                };
            };
        };
    };
    confirmCurrentCancellation: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CancellationConfirmationRequest"];
            };
        };
        responses: {
            /** @description Solicitud confirmada o repetición idempotente */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CancellationExecutionResponse"];
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
    downloadCurrentCancellationReceipt: {
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
    getCurrentCancellationOutcome: {
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
                    "application/json": components["schemas"]["CancellationExecutionResponse"];
                };
            };
        };
    };
    getCurrentRequestCertificates: {
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
                    "application/json": components["schemas"]["CertificateListResponse"];
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
}
