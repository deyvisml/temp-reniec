# Frontend-Backend Integration Specification

## Purpose

Define the reusable, versioned, observable, and tested technical communication between the Next.js frontend, Spring Boot backend, and MySQL.

## Requirements

### Requirement: Versioned technical integration API
The backend SHALL reserve `/api/v1` for application APIs and SHALL expose `GET /api/v1/system/status` as the only new application endpoint in this change. A successful response SHALL use HTTP 200 JSON containing controlled `status`, `database`, and UTC `timestamp` fields, and MUST NOT expose database coordinates, credentials, schema details, queries, versions, personal data, or internal implementation information.

#### Scenario: Integrated stack is available
- **WHEN** a client requests `GET /api/v1/system/status` while the backend can query MySQL
- **THEN** the response is HTTP 200, identifies both overall and database status as `UP`, contains a UTC timestamp, and includes `X-Correlation-ID`

#### Scenario: API namespace is inspected
- **WHEN** backend application routes introduced by this change are reviewed
- **THEN** the technical status route is under `/api/v1`, Actuator and OpenAPI retain their technical paths, and no citizen-flow endpoint exists

### Requirement: Real and safe MySQL availability check
The status operation SHALL execute a lightweight `SELECT 1` through the backend's configured datasource on every explicit request. A database access failure SHALL produce HTTP 503 using the common `ApiError` with code `DEPENDENCY_UNAVAILABLE`, a generic Spanish message, UTC timestamp, request path, and correlation identifier; responses and logs MUST NOT reveal the SQL exception, JDBC URL, query, credentials, or database internals.

#### Scenario: Database is unavailable after backend startup
- **WHEN** the status endpoint cannot complete its MySQL check
- **THEN** the client receives a safe HTTP 503 common error with correlation and no internal connection detail

### Requirement: Restricted configurable CORS
The backend SHALL apply CORS to `/api/**` using an environment-configurable exact origin list. Local configuration SHALL allow `http://localhost:3000`; allowed methods SHALL be limited to `GET`, `POST`, and `OPTIONS`; allowed request headers SHALL be limited to `Accept`, `Content-Type`, and `X-Correlation-ID`; credentials SHALL be allowed for future cookies; and `X-Correlation-ID` SHALL be exposed. Wildcard origins and reflected unvalidated origins MUST NOT be used.

#### Scenario: Local frontend sends a preflight request
- **WHEN** an OPTIONS preflight for an allowed API method comes from `http://localhost:3000`
- **THEN** the backend returns CORS headers for that exact origin, permits credentials, and exposes the allowed method and headers

#### Scenario: Unknown origin sends a request
- **WHEN** a CORS request comes from an origin not present in the configured list
- **THEN** the backend does not grant that origin cross-origin access

#### Scenario: Correlation response header is read in the browser
- **WHEN** the allowed frontend receives an API response
- **THEN** browser code can read `X-Correlation-ID` through the CORS exposed-header policy

### Requirement: OpenAPI contract as backend source of truth
The backend SHALL generate OpenAPI 3 documentation from its `/api/v1/**` controllers and DTOs at `/v3/api-docs` using an API-only Springdoc module compatible with Spring Boot 4. The document SHALL describe status success, the common error schema, HTTP 503, content types, timestamps, and `X-Correlation-ID`; it MUST NOT include Actuator, test-only routes, future citizen endpoints, Swagger UI, secrets, or database details. OpenAPI SHALL be enabled in local and test configuration while production exposure remains deferred.

#### Scenario: OpenAPI document is requested locally
- **WHEN** a contributor requests `/v3/api-docs` with the local profile
- **THEN** the document contains `/api/v1/system/status` and its referenced schemas and contains no Actuator or test-only path

### Requirement: Generated and synchronized TypeScript contracts
The frontend SHALL use `openapi-typescript` as development-only tooling to generate committed TypeScript types from a canonical snapshot obtained from the running backend OpenAPI document. It SHALL provide `api:sync` to update the snapshot and types and `api:check` to fail when committed artifacts differ from the current backend contract. Application contract aliases SHALL derive from generated types rather than duplicate backend DTOs manually, and normal frontend build and unit tests SHALL not require the backend to be running.

#### Scenario: Contract is synchronized
- **WHEN** a contributor runs the documented synchronization command against the local backend
- **THEN** the canonical OpenAPI snapshot and generated TypeScript file are updated deterministically

#### Scenario: Backend contract drifts
- **WHEN** the runtime OpenAPI differs from the committed snapshot or generated types
- **THEN** the contract-check command exits unsuccessfully and instructs the contributor to synchronize and review the contract

### Requirement: Reusable browser and server HTTP transport
The central frontend client SHALL use native `fetch`, SHALL choose server-only `BACKEND_URL` during server execution and non-sensitive `NEXT_PUBLIC_BACKEND_URL` in the browser, and SHALL preserve caller options. It SHALL request JSON, use `credentials: "include"`, generate and send a valid `X-Correlation-ID` unless supplied, expose the returned identifier, enforce an 8-second default timeout, respect caller cancellation, and handle JSON success, empty success, structured HTTP errors, non-JSON errors, invalid success bodies, network failures, timeouts, and cancellation through typed `HttpClientError`. It MUST NOT implement automatic retries, interceptors, JWT, refresh, session persistence, body logging, or a third-party HTTP library.

#### Scenario: Correlated request succeeds
- **WHEN** the client sends a request without caller correlation and receives successful JSON
- **THEN** it generates and sends a valid correlation identifier and returns the response data with the identifier selected by the backend

#### Scenario: Request reaches timeout
- **WHEN** no response completes within the configured default timeout
- **THEN** the client aborts fetch and throws a generic `HttpClientError` with code `TIMEOUT`

#### Scenario: Caller cancels a request
- **WHEN** the caller's abort signal is triggered before completion
- **THEN** the client stops the request and reports `REQUEST_ABORTED` without treating it as a timeout

#### Scenario: Backend returns common error
- **WHEN** the backend returns a JSON `ApiError`
- **THEN** the client preserves its public code, message, HTTP status, and response-header correlation identifier

#### Scenario: Network or response format fails
- **WHEN** fetch fails before a response or a successful response cannot be interpreted according to its contract
- **THEN** the client reports `NETWORK_ERROR` or `INVALID_RESPONSE` with a generic message and no native exception detail

### Requirement: Functional eligibility contract uses the shared transport
The citizen eligibility client SHALL use the existing centralized JSON transport, environment-based backend URL, credentials mode, timeout, abort handling, correlation propagation, and common error mapping. Its request and response types SHALL come from the generated OpenAPI declarations rather than handwritten duplicate DTOs.

#### Scenario: Eligibility request succeeds
- **WHEN** the frontend submits a valid DNI and the backend returns a functional outcome
- **THEN** the client returns typed outcome data and the response correlation identifier to the feature without duplicating transport behavior

#### Scenario: Eligibility request fails
- **WHEN** the backend returns a common API error, times out, sends invalid JSON, or cannot be reached
- **THEN** the shared transport produces the established typed client error and the feature maps it to the appropriate citizen state

### Requirement: Layered and real integration verification
The project SHALL retain fast infrastructure-free backend and frontend suites and SHALL add separate verification for the integration contract. Backend integration tests SHALL use disposable MySQL Testcontainers to verify status, failure handling, correlation, CORS, and OpenAPI. A separately invoked frontend integration suite SHALL use the real central client and generated types against a running local backend connected to MySQL. Documentation SHALL provide the exact startup, contract synchronization, unit-test, integration-test, build, health, and shutdown commands.

#### Scenario: Isolated suites run without the full stack
- **WHEN** contributors run the normal backend and frontend unit-test commands
- **THEN** the existing fast suites pass without requiring the other application or the local Compose database

#### Scenario: Real integration suite is executed
- **WHEN** MySQL, backend, and the documented frontend integration command are running with local configuration
- **THEN** the real frontend client obtains an `UP` response backed by MySQL and validates correlation and generated contract types without a mocked fetch

### Requirement: Integration-only scope boundary
This change MUST NOT add DNI input or validation, eligibility or certificate lookup behavior, cancellation-request creation, JWT, refresh tokens, progress recovery, ID Perú, reasons, confirmation, revocation, receipts, administrative features, external-service contracts, automatic retries, or production deployment configuration.

#### Scenario: Completed integration is reviewed
- **WHEN** routes, UI, dependencies, OpenAPI, generated types, and tests are inspected
- **THEN** they implement only the reusable technical integration and no citizen or administrative functionality
