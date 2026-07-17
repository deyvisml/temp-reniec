export interface paths {
    "/api/v1/system/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Comprueba la disponibilidad del backend y MySQL */
        get: operations["getStatus"];
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
        /** @description Estado técnico del backend y su conexión con MySQL */
        SystemStatusResponse: {
            /** @example UP */
            status?: string;
            /** @example UP */
            database?: string;
            /** Format: date-time */
            timestamp?: string;
        };
        ApiError: {
            code?: string;
            message?: string;
            /** Format: date-time */
            timestamp?: string;
            path?: string;
            correlationId?: string;
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
    getStatus: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Backend y base de datos disponibles */
            200: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: unknown;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SystemStatusResponse"];
                };
            };
            /** @description Dependencia técnica no disponible */
            503: {
                headers: {
                    /** @description Identificador de correlación de la solicitud */
                    "X-Correlation-ID"?: unknown;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiError"];
                };
            };
        };
    };
}
