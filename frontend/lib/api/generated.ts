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
         * Inicia o recupera una solicitud y consulta su elegibilidad
         * @description Valida el DNI, recupera una solicitud compatible cuando existe o crea una nueva, y consulta si hay certificados digitales susceptibles de cancelación. No devuelve certificados individuales ni expone el DNI completo.
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
             * @example /api/v1/cancellation-requests
             */
            path: string;
            /**
             * Format: date-time
             * @description Fecha y hora UTC del error.
             * @example 2026-07-20T18:30:00Z
             */
            timestamp: string;
        };
        /** @description Resultado normalizado del inicio o recuperación de una solicitud de cancelación. */
        CancellationRequestResponse: {
            /**
             * @description Indica si el backend autoriza continuar al siguiente paso.
             * @example true
             */
            canContinue: boolean;
            /**
             * @description Resultado normalizado de la consulta de certificados.
             * @example ELIGIBLE
             * @enum {string}
             */
            eligibilityResult: "ELIGIBLE" | "NOT_ELIGIBLE" | "UNAVAILABLE" | "INCONCLUSIVE" | "ERROR";
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
            requestStatus: "STARTED" | "CHECKING_ELIGIBILITY" | "NOT_ELIGIBLE" | "ELIGIBLE" | "PENDING_IDENTITY_VERIFICATION" | "IDENTITY_VERIFIED" | "REASON_REGISTERED" | "PENDING_CONFIRMATION" | "CONFIRMED" | "REVOCATION_IN_PROGRESS" | "COMPLETED" | "FAILED" | "OUTCOME_UNKNOWN" | "RECEIPT_AVAILABLE" | "ABANDONED";
            /**
             * @description Indica si se recuperó una solicitud compatible existente.
             * @example false
             */
            reused: boolean;
        };
        /** @description Datos requeridos para iniciar o recuperar una solicitud y consultar su elegibilidad. */
        StartCancellationRequest: {
            /** @description Número de DNI del ciudadano. Debe contener exactamente ocho dígitos ASCII. Por privacidad, la documentación no incluye un DNI completo de ejemplo. */
            dni: string;
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
        /** @description DNI que inicia o recupera la solicitud. Se valida nuevamente en el backend. */
        requestBody: {
            content: {
                "application/json": components["schemas"]["StartCancellationRequest"];
            };
        };
        responses: {
            /** @description Resultado normalizado de elegibilidad */
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
            /** @description DNI, JSON o cuerpo inválido */
            400: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Consulta en curso o conflicto concurrente */
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
            /** @description Error controlado del proveedor de elegibilidad */
            502: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Servicio de elegibilidad no disponible */
            503: {
                headers: {
                    "X-Correlation-ID"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
            /** @description Tiempo de espera agotado */
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
}
