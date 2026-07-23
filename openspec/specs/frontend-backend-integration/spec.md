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
The backend SHALL generate OpenAPI 3 documentation from every exposed `/api/v1/**` controller and the single exposed Actuator health operation. The cancellation-request operation SHALL describe the initial certificate-existence query, normalized availability outcomes, validation, correlation, success and controlled errors. It MUST NOT describe a raw external boolean, certificate collection, count, order number, creation date, UUID, second-service contract or security mechanism that has not been implemented.

#### Scenario: OpenAPI document is requested locally
- **WHEN** a contributor requests `/v3/api-docs` with the local profile
- **THEN** `POST /api/v1/cancellation-requests` documents availability-only semantics and contains no certificate-level response schema

#### Scenario: Documentation is organized
- **WHEN** the generated document is inspected
- **THEN** the operation and DTO fields distinguish certificate availability from the future detailed listing

#### Scenario: Security has not been implemented
- **WHEN** OpenAPI components and operations are inspected before JWT exists
- **THEN** the document contains no bearer, OAuth2, JWT or other unimplemented security scheme

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
The initial certificate-availability client SHALL use the centralized JSON transport, environment-based backend URL, credentials mode, timeout, abort handling, correlation propagation and common error mapping. Request and response types SHALL come from generated OpenAPI declarations. Each submission SHALL include the current in-memory `recaptchaToken` with the DNI, while the client MUST NOT persist or log that token, accept or synthesize certificate collections, or lose the distinction between CAPTCHA failure, confirmed certificate absence and transport/service failure.

#### Scenario: Availability request succeeds
- **WHEN** the frontend submits a valid DNI and current CAPTCHA token and the backend returns `AVAILABLE` or `NOT_AVAILABLE`
- **THEN** the client returns typed availability data and correlation without duplicating transport behavior, retaining the token or exposing certificate details

#### Scenario: CAPTCHA request fails
- **WHEN** the backend returns a controlled CAPTCHA rejection, expiration, timeout, unavailability or invalid-response error
- **THEN** the shared transport preserves the stable code and correlation so the feature resets the widget and does not invoke or infer an availability outcome

#### Scenario: Availability transport fails
- **WHEN** the backend returns another controlled error, times out, sends invalid JSON or cannot be reached
- **THEN** the shared transport produces the established typed error and the feature maps it neither to CAPTCHA success nor `NOT_AVAILABLE`

### Requirement: Layered and real integration verification
The project SHALL retain fast infrastructure-free backend and frontend suites and separate verification for OpenAPI and MySQL integration. Documentation tests SHALL verify the availability operation metadata, DTO fields, allowed values, validation constraints, correlation and errors. Testcontainers SHALL verify real availability persistence with zero certificate rows. The frontend contract check SHALL compare the committed OpenAPI snapshot and generated TypeScript types with the backend document.

#### Scenario: Endpoint contract changes without documentation
- **WHEN** availability fields, values, states, errors or privacy rules change without updating OpenAPI and derived artifacts
- **THEN** documentation or contract-drift verification fails

#### Scenario: Initial response schema is inspected
- **WHEN** automated checks traverse the cancellation-request success schema
- **THEN** they find `availabilityResult` and no array, count, order number, creation date or UUID property

#### Scenario: Existing verification suites run
- **WHEN** Maven, frontend tests and contract checks execute with documented prerequisites
- **THEN** backend behavior, MySQL persistence, Swagger assertions and generated TypeScript declarations agree on the existence-only contract

### Requirement: Integration-only scope boundary
This change MUST NOT add DNI input or validation, eligibility or certificate lookup behavior, cancellation-request creation, JWT, refresh tokens, progress recovery, ID Perú, reasons, confirmation, revocation, receipts, administrative features, external-service contracts, automatic retries, or production deployment configuration.

#### Scenario: Completed integration is reviewed
- **WHEN** routes, UI, dependencies, OpenAPI, generated types, and tests are inspected
- **THEN** they implement only the reusable technical integration and no citizen or administrative functionality

### Requirement: Complete human-readable operation and DTO documentation
Every current API operation SHALL document its purpose, inputs, successful result, relevant controlled errors, HTTP codes and reusable schemas. The cancellation-request DTO SHALL identify `availabilityResult` as the normalized result of the first service, explain that `AVAILABLE` permits identity verification but does not mean the detailed list has been loaded, and use only fictitious non-sensitive examples.

#### Scenario: Cancellation initiation is inspected
- **WHEN** a contributor opens `POST /api/v1/cancellation-requests` in Swagger UI
- **THEN** the operation explains the existence-only query, shows no individual certificate model and documents each functional or technical result accurately

#### Scenario: Error models are inspected
- **WHEN** unavailable, timeout or technical error responses are reviewed
- **THEN** Swagger presents the common safe error and never documents those conditions as confirmed certificate absence

### Requirement: OpenAPI maintenance is part of endpoint completion
No new or modified endpoint SHALL be considered complete unless its OpenAPI operation, DTO schemas, validation, response codes, controlled errors, correlation behavior, privacy boundary, generated snapshot and contract tests are synchronized with runtime behavior. The future second-service endpoint SHALL document detailed certificate fields only in the increment that implements that service after authentication.

#### Scenario: Initial endpoint is completed
- **WHEN** this correction is verified
- **THEN** runtime JSON, Swagger UI, machine-readable OpenAPI, TypeScript declarations and frontend behavior all use the same availability-only contract

#### Scenario: Future listing endpoint is introduced
- **WHEN** a later increment implements the post-authentication certificate list
- **THEN** that increment adds its own operation and schemas rather than extending the unauthenticated initial response

### Requirement: Identity API contracts are versioned and synchronized
The backend SHALL expose versioned contracts for starting identity verification, processing the provider callback, reading current identity state and invalidating local authorization. OpenAPI SHALL document request/response models, form fields, cookies, status codes and normalized errors, and generated TypeScript types SHALL be regenerated from the validated contract.

#### Scenario: Developer inspects Swagger UI
- **WHEN** the identity API group is opened
- **THEN** each operation, state, field, cookie effect and principal error is described without exposing secrets or suggesting nonexistent security mechanisms

#### Scenario: Contract generation runs
- **WHEN** OpenAPI changes are accepted
- **THEN** frontend generated types match the backend and handwritten duplicate DTOs are not introduced

### Requirement: Frontend HTTP supports secure credential continuity
The centralized HTTP client SHALL support `credentials: include` for same-project identity and protected-flow calls, retain correlation identifiers and existing JSON/error handling, and handle the provider callback only through browser navigation. It MUST NOT attempt to read, copy or persist the HttpOnly cookie.

#### Scenario: Identity start succeeds
- **WHEN** the frontend calls the start endpoint with a valid continuation cookie
- **THEN** it receives only the authorization URL and correlation-safe response data before navigating

#### Scenario: Protected call is unauthorized
- **WHEN** the cookie is absent, invalid, expired or revoked
- **THEN** the client maps the standard API error to a controlled restart message without exposing raw token details

### Requirement: Identity integration errors remain normalized
Provider rejection, cancellation, mismatch, expired/replayed state, invalid callback, token failure, JWT failure, timeout, unavailability, invalid configuration and unauthorized flow access SHALL use stable backend codes and the existing common error envelope. Provider payloads and exceptions MUST NOT be returned to the browser.

#### Scenario: Provider service times out
- **WHEN** a real token, userinfo or JWKS call exceeds its timeout
- **THEN** the client receives a stable temporary-error code, timestamp, path and correlation ID but no endpoint credentials or token material

#### Scenario: Identity differs
- **WHEN** the authenticated DNI does not match the request
- **THEN** the client receives a dedicated controlled outcome without either DNI value
