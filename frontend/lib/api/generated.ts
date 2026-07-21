export interface paths {
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
            requestStatus: "STARTED" | "CHECKING_AVAILABILITY" | "NO_CERTIFICATES_AVAILABLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "AUTHENTICATED_PENDING_CERTIFICATE_LIST" | "CERTIFICATES_AVAILABLE" | "CERTIFICATES_SELECTED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "REVOCATION_SUCCEEDED" | "REVOCATION_FAILED" | "REVOCATION_OUTCOME_UNKNOWN" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
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
