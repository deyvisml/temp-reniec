# Frontend-Backend Integration Specification

## Purpose

Define the reusable, versioned, observable, and tested technical communication between the Next.js frontend, Spring Boot backend, and MySQL.

> **Implementation alignment notice (SPEC-08):** The transport foundation remains current, but its existing eligibility contract reflects the implemented binary response. `docs/context/PROJECT_CONTEXT.md` is authoritative for the new list-based lookup and later selection flow. Updating DTOs, OpenAPI declarations, generated TypeScript types, and integration tests belongs to a subsequent functional change.
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
The backend SHALL generate OpenAPI 3 documentation from every exposed `/api/v1/**` controller and from the single exposed Actuator health operation using Springdoc OpenAPI 3.0.3 compatible with Spring Boot 4. The document SHALL describe API metadata, functional tags, operation purpose, request parameters and bodies, media types, validation constraints, success responses, controlled error responses, HTTP codes, UTC timestamps, and `X-Correlation-ID` where applicable. It MUST NOT include test-only routes, non-exposed Actuator operations, future citizen endpoints, secrets, full DNI values, database details, provider payloads, or security schemes that have not been implemented. OpenAPI SHALL be enabled in local and test configuration while default and production exposure remain disabled or deferred.

#### Scenario: OpenAPI document is requested locally
- **WHEN** a contributor requests `/v3/api-docs` or `/v3/api-docs.yaml` with the local profile
- **THEN** the document contains `GET /api/v1/system/status`, `POST /api/v1/cancellation-requests`, and `GET /actuator/health` with their referenced schemas and contains no test-only or non-exposed operation

#### Scenario: Documentation is organized
- **WHEN** the generated document is inspected
- **THEN** cancellation-request operations and technical-status operations use clear module-oriented tags and every operation has a summary, description, stable operation identifier, documented content, and actual HTTP responses

#### Scenario: Security has not been implemented
- **WHEN** the OpenAPI components and operations are inspected before JWT exists
- **THEN** the document contains no bearer, OAuth2, authorization, JWT, refresh-token, or other security scheme or requirement

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
The project SHALL retain fast infrastructure-free backend and frontend suites and SHALL maintain separate verification for the integration contract. Backend documentation tests SHALL use the generated OpenAPI document to verify route coverage, operation metadata, tags, input and output schemas, validation constraints, content types, correlation headers, and documented HTTP responses; an isolated test SHALL verify Swagger UI availability under development configuration. Existing MySQL Testcontainers tests SHALL continue verifying real status and eligibility behavior. The frontend contract check SHALL compare the committed snapshot and generated TypeScript types with the backend document. Documentation SHALL provide the exact startup, contract synchronization, unit-test, integration-test, build, health, Swagger UI, and shutdown commands.

#### Scenario: Current route inventory is checked
- **WHEN** backend documentation tests compare exposed application mappings and the configured Actuator health path with `/v3/api-docs`
- **THEN** every current endpoint is represented exactly once and no test-only or unavailable route is presented as public API

#### Scenario: Endpoint contract changes without documentation
- **WHEN** a contributor adds or modifies a route, DTO, validation, media type, response code, error, or implemented security rule without updating its OpenAPI representation and committed derived artifacts
- **THEN** the documentation or contract-drift verification fails before the endpoint is considered complete

#### Scenario: Existing verification suites run
- **WHEN** the complete Maven and frontend contract checks execute with their documented prerequisites
- **THEN** the backend compiles, existing behavior remains green, Swagger/OpenAPI assertions pass, and generated TypeScript declarations match the current backend contract

### Requirement: Integration-only scope boundary
This change MUST NOT add DNI input or validation, eligibility or certificate lookup behavior, cancellation-request creation, JWT, refresh tokens, progress recovery, ID Perú, reasons, confirmation, revocation, receipts, administrative features, external-service contracts, automatic retries, or production deployment configuration.

#### Scenario: Completed integration is reviewed
- **WHEN** routes, UI, dependencies, OpenAPI, generated types, and tests are inspected
- **THEN** they implement only the reusable technical integration and no citizen or administrative functionality

### Requirement: Complete human-readable operation and DTO documentation
Every currently implemented API operation SHALL document its purpose, inputs, request body when present, successful result, relevant controlled errors, HTTP codes, and reusable schemas. Request and response DTO schemas SHALL identify required fields, formats, allowed values, validation limits, nullability where relevant, and fictitious non-sensitive examples that match runtime serialization.

#### Scenario: Cancellation initiation is inspected
- **WHEN** a contributor opens `POST /api/v1/cancellation-requests` in Swagger UI
- **THEN** the operation explains initiation or recovery and eligibility, shows the validated DNI request without real personal data, documents the normalized response, correlation header, and every controlled status currently produced by the endpoint

#### Scenario: Technical status is inspected
- **WHEN** a contributor opens `GET /api/v1/system/status` or `GET /actuator/health` in Swagger UI
- **THEN** each operation explains its distinct purpose, successful status representation, dependency failure behavior where applicable, and exposes no database coordinates or internal health details

#### Scenario: Error models are inspected
- **WHEN** an operation references a handled error response
- **THEN** Swagger UI presents the common `ApiError` fields, formats and safe examples consistently with `GlobalExceptionHandler`

### Requirement: OpenAPI maintenance is part of endpoint completion
No new or modified endpoint SHALL be considered complete unless its OpenAPI operation, DTO schemas, validation constraints, response codes, controlled errors, correlation behavior, implemented security rules, generated snapshot, and contract tests are synchronized with runtime behavior. Security documentation MUST be added only in the increment that implements the corresponding security mechanism.

#### Scenario: Pull request introduces an endpoint
- **WHEN** a future change introduces or modifies an HTTP operation
- **THEN** its task list and verification include OpenAPI documentation, contract coverage, and synchronization of derived frontend artifacts

#### Scenario: Future JWT security is implemented
- **WHEN** a later increment adds an actual JWT authentication contract
- **THEN** that same increment updates OpenAPI security schemes, protected operations, Swagger behavior and tests rather than documenting them in advance

