## MODIFIED Requirements

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

## ADDED Requirements

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
