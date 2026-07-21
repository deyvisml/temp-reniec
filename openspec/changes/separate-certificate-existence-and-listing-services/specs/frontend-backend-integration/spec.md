## MODIFIED Requirements

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

### Requirement: Functional eligibility contract uses the shared transport
The initial certificate-availability client SHALL use the centralized JSON transport, environment-based backend URL, credentials mode, timeout, abort handling, correlation propagation and common error mapping. Request and response types SHALL come from generated OpenAPI declarations. The client MUST NOT accept or synthesize certificate collections and MUST preserve the distinction between confirmed absence and transport or service failure.

#### Scenario: Availability request succeeds
- **WHEN** the frontend submits a valid DNI and the backend returns `AVAILABLE` or `NOT_AVAILABLE`
- **THEN** the client returns typed availability data and correlation without duplicating transport behavior or exposing certificate details

#### Scenario: Availability request fails
- **WHEN** the backend returns a controlled error, times out, sends invalid JSON or cannot be reached
- **THEN** the shared transport produces the established typed error and the feature does not map it to `NOT_AVAILABLE`

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
