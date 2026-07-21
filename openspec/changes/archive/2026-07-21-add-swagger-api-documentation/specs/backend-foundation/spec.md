## MODIFIED Requirements

### Requirement: Minimal managed dependencies
The backend SHALL use Spring Boot's managed web MVC, validation, Actuator, Spring Data JPA, Flyway MySQL, and MySQL driver support; Springdoc OpenAPI's WebMVC UI starter 3.0.3 required to publish and explore the implemented API contract; plus the minimum Spring Boot and MySQL Testcontainers test support required by the implemented foundation, cancellation-request persistence model, technical integration, and API-documentation verification. It MUST NOT add a second OpenAPI generator, a UI kit unrelated to Swagger, security, messaging, cache, alternate database, external-integration, distributed-tracing, cryptographic, document-generation, SDK-generation, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for HTTP serving, validation, health, MySQL persistence and migrations, OpenAPI and Swagger UI publication, or the specified tests, the API-only and UI springdoc starters do not coexist, and no out-of-scope starter is present

### Requirement: Concise local operation documentation
The backend SHALL contain a concise README covering prerequisites, one-time creation of ignored `backend/.env` from the committed local example, local MySQL lifecycle, Maven Wrapper build and verification commands, health and application API URLs, `/v3/api-docs`, `/v3/api-docs.yaml`, the local Swagger UI URL, profile-specific documentation exposure, supported environment variables, Flyway ownership, persistence-test requirements, correlation, sensitive-data restrictions, and the rule that every new or modified endpoint MUST update OpenAPI documentation and its contract tests before it is complete. It SHALL state that Swagger UI is a local development tool, that no authentication scheme is documented until one exists, and that production exposure remains deferred.

#### Scenario: Contributor explores the local API
- **WHEN** a contributor starts MySQL and the backend with the documented local profile
- **THEN** the README enables the contributor to open Swagger UI, inspect the machine-readable documents, identify all current operations and execute the endpoints that accept direct local testing

#### Scenario: Contributor reviews production boundaries
- **WHEN** a contributor reads the documentation-exposure guidance
- **THEN** it is clear that OpenAPI and Swagger UI are disabled by default, production exposure is undecided, and no nonexistent security mechanism is represented

## ADDED Requirements

### Requirement: Development-only Swagger UI exposure
The backend SHALL provide Swagger UI at the documented path when the `local` profile is active and SHALL keep Swagger UI and OpenAPI disabled in common configuration. The test profile SHALL expose the machine-readable OpenAPI document for automated verification while keeping the interactive UI disabled unless a dedicated test explicitly enables it. This change MUST NOT define production exposure.

#### Scenario: Local developer opens Swagger UI
- **WHEN** the backend runs with the `local` profile and valid MySQL configuration
- **THEN** the documented Swagger UI URL loads successfully and consumes the generated OpenAPI document

#### Scenario: Backend runs without a development profile
- **WHEN** the backend runs using common configuration without an explicit documentation-enabled profile
- **THEN** neither Swagger UI nor the OpenAPI document is exposed

#### Scenario: Automated contract tests run
- **WHEN** the backend test profile executes documentation tests
- **THEN** `/v3/api-docs` is available to the tests and Swagger UI remains disabled except in the isolated UI availability test
