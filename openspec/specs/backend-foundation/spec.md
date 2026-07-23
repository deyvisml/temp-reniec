
# Backend Foundation Specification

## Purpose

Define the minimal, executable, maintainable, and non-functional technical foundation for the system backend.
## Requirements
### Requirement: Executable single-module backend project
The repository SHALL contain a single-module Maven project at `/backend` using Java 21, Spring Boot 4.1.0, executable JAR packaging, Maven Wrapper 3.9.16, and the institutional base package `pe.gob.reniec.certificados.cancelacion`. The project SHALL compile without external services and SHALL start when its configured MySQL database is available and Flyway has successfully validated or migrated the schema.

#### Scenario: Backend is built from a clean checkout
- **WHEN** a contributor with Java 21 runs the documented Maven Wrapper build from `/backend`
- **THEN** Maven compiles the application and produces an executable Spring Boot JAR without contacting an external functional service

#### Scenario: Backend starts locally with MySQL
- **WHEN** a contributor starts the packaged application or uses the documented Spring Boot Maven command with valid MySQL configuration
- **THEN** Flyway validates or migrates the schema and the Spring application context and embedded web server start successfully

#### Scenario: Required database is unavailable
- **WHEN** the backend starts with an unavailable or invalid MySQL connection
- **THEN** startup fails in a controlled manner without exposing credentials or silently creating an alternative persistence mechanism

### Requirement: Minimal managed dependencies
The backend SHALL use Spring Boot's managed web MVC, validation, Actuator, Spring Data JPA, Flyway MySQL, and MySQL driver support; Springdoc OpenAPI's WebMVC UI starter 3.0.3 required to publish and explore the implemented API contract; plus the minimum Spring Boot and MySQL Testcontainers test support required by the implemented foundation, cancellation-request persistence model, technical integration, and API-documentation verification. It MUST NOT add a second OpenAPI generator, a UI kit unrelated to Swagger, security, messaging, cache, alternate database, external-integration, distributed-tracing, cryptographic, document-generation, SDK-generation, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for HTTP serving, validation, health, MySQL persistence and migrations, OpenAPI and Swagger UI publication, or the specified tests, the API-only and UI springdoc starters do not coexist, and no out-of-scope starter is present

### Requirement: Simple feature-oriented structure
The backend SHALL place the application class at the root of the institutional package and SHALL create only packages and classes used by the current technical foundation. Shared error and web concerns MAY be grouped under focused packages, while empty cancellation, domain, application, persistence, integration, or API layers MUST NOT be created.

#### Scenario: Initial package tree is inspected
- **WHEN** a contributor reviews the production source tree
- **THEN** every package contains a component used by this change and no speculative architecture or empty functional module exists

### Requirement: Externalized local and test configuration
The backend SHALL provide common configuration plus distinct `local` and `test` profiles using Spring Boot configuration files. The `local` profile SHALL optionally import `backend/.env` as a properties-formatted Config Data resource so a committed local template can be copied once and shared with Docker Compose; process environment values SHALL remain able to override imported values. The local Compose template and Spring local fallback SHALL use host port `3307` by default while the MySQL container continues to listen on `3306`, and `DB_PORT` SHALL allow a developer to select another host port without editing versioned YAML. MySQL host, port, database name, user, and password SHALL be supplied or overridable through documented environment variables; the `test` profile SHALL receive disposable Testcontainers connection properties; local instructions SHALL activate `local`; and `backend/.env`, production secrets, and production credentials MUST NOT be committed. Hibernate SHALL validate rather than generate the schema, Flyway SHALL own schema changes, all persisted application dates SHALL use UTC semantics, and production configuration SHALL remain outside this change.

#### Scenario: Compose and native MySQL use different host ports
- **WHEN** a contributor starts a native MySQL installation on host port `3306` and starts the documented Compose environment with the default local template
- **THEN** Compose publishes its MySQL service on host port `3307`, maps it to container port `3306`, and no host-port conflict occurs

#### Scenario: Local profile uses the private environment file
- **WHEN** a contributor starts the backend from `/backend` with the `local` profile and a `.env` copied from the documented local template
- **THEN** common and local configuration are applied, Spring connects to the Compose database through host port `3307`, Flyway validates or migrates it, and the application starts

#### Scenario: Contributor selects another local port
- **WHEN** host port `3307` is unavailable and the contributor changes only `DB_PORT` in `backend/.env` to another free port
- **THEN** Compose publishes that port and Spring Boot connects through the same port without a versioned configuration change

#### Scenario: Local environment value overrides the file
- **WHEN** a documented process environment variable such as `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT`, or `LOG_LEVEL_APP` is supplied while `.env` is present
- **THEN** Spring Boot uses the process environment value instead of the imported file value or any safe repository default

#### Scenario: Optional local file is not used
- **WHEN** `.env` is absent and valid documented MySQL environment values are supplied directly to the process without `DB_PORT`
- **THEN** the optional import does not block startup and the local profile uses host port `3307` as its safe local fallback

#### Scenario: Test profile is used
- **WHEN** the persistence integration suite starts the application
- **THEN** the `test` profile uses the disposable MySQL Testcontainer configuration and no manually installed MySQL, local `.env`, Compose database, or production-only resource is required

#### Scenario: Schema ownership is inspected
- **WHEN** persistence configuration is reviewed
- **THEN** Flyway is the schema owner and Hibernate is limited to validating that JPA mappings match the migrated schema

### Requirement: Minimal operational health endpoint
The running backend SHALL expose Spring Boot Actuator health at `/actuator/health`, SHALL include the standard database contributor in aggregate health, and SHALL expose no other Actuator endpoint over HTTP. A healthy response SHALL indicate that the application and configured MySQL connection are operational, while health details MUST NOT reveal JDBC URLs, credentials, schema internals, queries, or external-service information to the client.

#### Scenario: Operational application and database respond to health check
- **WHEN** a client sends `GET /actuator/health` to a running backend connected to MySQL
- **THEN** the server returns HTTP 200 with aggregate Actuator status `UP`

#### Scenario: Database becomes unavailable
- **WHEN** the running application's database health contributor cannot reach MySQL
- **THEN** aggregate health does not report `UP` and the public body contains no connection secrets or internal database details

#### Scenario: Actuator exposure is limited
- **WHEN** the management endpoint configuration is inspected or a non-health Actuator endpoint is requested
- **THEN** only `health` is exposed over HTTP and component details are not returned publicly

### Requirement: Common API error representation
The backend SHALL represent handled HTTP errors as JSON containing `code`, a comprehensible `message`, an ISO-8601 UTC `timestamp`, the request `path`, and `correlationId`. The global error handler SHALL cover at least request validation, malformed requests, unsupported methods, routed not-found errors, and unexpected failures with suitable 4xx or 500 statuses. Client responses MUST NOT contain stack traces, class names, internal exception messages, credentials, tokens, biometric data, full DNI values, or implementation details.

#### Scenario: Validation error is represented consistently
- **WHEN** a controlled request fails Jakarta Bean Validation
- **THEN** the response uses HTTP 400 and contains all common error fields with a stable public code and message

#### Scenario: Unexpected error is hidden from the client
- **WHEN** an unhandled server exception reaches the global handler
- **THEN** the response uses HTTP 500 with a generic public code and message and contains no trace or internal exception detail

### Requirement: Request correlation lifecycle
The backend SHALL use the `X-Correlation-ID` request and response header. A client value SHALL be accepted only when it has 1 to 64 ASCII characters and matches `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`; otherwise the backend SHALL generate a UUID. The selected value SHALL be available as a request attribute, included in MDC under `correlationId`, returned in the response header, available to the error representation, and removed from MDC after the request completes.

#### Scenario: Missing correlation is generated
- **WHEN** a request does not include `X-Correlation-ID`
- **THEN** the backend generates a correlation identifier and returns the same value in the response header

#### Scenario: Valid client correlation is propagated
- **WHEN** a request includes a valid `X-Correlation-ID`
- **THEN** the exact supplied value is used for the request and returned in the response header and any error body

#### Scenario: Invalid client correlation is replaced
- **WHEN** a request includes an empty, oversized, non-ASCII, whitespace-containing, or otherwise invalid `X-Correlation-ID`
- **THEN** the backend ignores it, generates a UUID, and returns the generated value

### Requirement: Correlated privacy-conscious logging
The backend SHALL use Spring Boot's default logging implementation with a pattern that includes the MDC correlation identifier. Request completion logs SHALL contain only the HTTP method, request path without query string, response status, and correlation identifier. Application code and documentation MUST prohibit logging full DNI values, tokens, credentials, biometric data, request bodies, authorization headers, and unnecessary personal data.

#### Scenario: Request produces a correlatable log
- **WHEN** the backend processes an HTTP request
- **THEN** request-scoped log entries can be associated through `correlationId` without recording query strings, headers, or request bodies

### Requirement: Validation support without domain rules
The backend SHALL enable Jakarta Bean Validation through Spring Boot so future request DTOs can use standard validation constraints. This change MUST NOT add DNI validation, cancellation-reason validation, or any other citizen-flow rule.

#### Scenario: Standard constraint is enforced in a controlled test
- **WHEN** a test-only request object violates a standard Jakarta validation constraint
- **THEN** Spring invokes the global validation error handling without relying on a domain-specific validator

### Requirement: Fast isolated baseline tests
The backend SHALL retain fast tests that verify context startup, the health response, common validation-error formatting, generated correlation, valid correlation propagation, and invalid correlation replacement. Tests that do not exercise persistence SHALL remain isolated from MySQL and external services. Persistence behavior SHALL be verified separately with disposable MySQL Testcontainers so the complete verification command requires Java 21 and a compatible container runtime but no manually installed database or external functional service.

#### Scenario: Baseline test subset is executed
- **WHEN** a contributor runs the documented fast test subset
- **THEN** the technical web-foundation tests pass without a database, container, or external network service

#### Scenario: Complete verification is executed
- **WHEN** a contributor runs the documented Maven verification command with a compatible container runtime available
- **THEN** baseline tests and MySQL persistence integration tests pass without a manually configured database or functional integration

### Requirement: Concise local operation documentation
The backend SHALL retain concise local instructions for MySQL, build, tests, health, OpenAPI and Swagger and SHALL document the deterministic fictitious DNI fixtures for the certificate-availability mock. The fixture table SHALL identify positive, negative, inconclusive, unavailable, timeout and technical-error behavior and SHALL state that no fixture returns certificate objects. It SHALL not document a second-service URL, payload or mock before that integration exists.

#### Scenario: Contributor tests the initial flow locally
- **WHEN** a contributor follows the README with the local profile
- **THEN** the contributor can reproduce every normalized availability scenario and understands that detailed certificates are not obtained at this stage

#### Scenario: Contributor searches for the listing service
- **WHEN** local backend documentation is inspected
- **THEN** the second service is identified as future work and no invented endpoint or environment variable is presented as current

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain MySQL, Flyway, Testcontainers, the corrected seven-table cancellation-request model, forward migrations through the availability correction, the versioned technical API, local MySQL Compose, Swagger/OpenAPI, the initial certificate-availability endpoint specified by `citizen-eligibility-entry`, and the Google reCAPTCHA v2 verification adapter specified by `initial-query-recaptcha-protection`. It MUST NOT persist CAPTCHA evidence or request sessions, add recovery windows or speculative guards, containerize the applications, implement JWT, ID Perú, the post-authentication listing service, its contract or attempt table, selection UI or endpoint, real revocation execution, document generation, production deployment, administration, microservices, queues, event sourcing, CQRS, another database or unused layers.

#### Scenario: Protected foundation is reviewed for scope
- **WHEN** routes, configuration, ports, adapters, OpenAPI, frontend contract and dependencies are inspected
- **THEN** the foundation validates CAPTCHA before the existence-only first service and preserves the certificate table for later listing without implementing any later flow

#### Scenario: Backend dependency set is reviewed
- **WHEN** Maven dependencies are compared before and after the change
- **THEN** Google verification uses Spring HTTP functionality already provided by the web starter and adds no preventive resilience, security or CAPTCHA SDK dependency

#### Scenario: Persistence schema is reviewed
- **WHEN** Flyway migrations and JPA entities are compared before and after the change
- **THEN** no table or column was added for CAPTCHA tokens, responses, IP addresses or challenge metadata

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

### Requirement: ID Perú configuration is external, validated and environment-specific
The backend SHALL bind typed ID Perú and flow-authorization properties for mode, credentials, provider URIs, issuer, redirect/return URIs, authentication mechanism, max age, Referer, timeouts, artifact TTLs, allowed algorithms and internal cryptographic keys. Real mode SHALL fail closed on missing or unsafe values; examples and documentation SHALL contain placeholders only.

#### Scenario: Local mock starts with no institutional credentials
- **WHEN** the local profile selects mock mode
- **THEN** the backend starts with deterministic fictitious configuration and makes no call to ID Perú

#### Scenario: Real mode is incomplete
- **WHEN** a mandatory secret, registered URI, issuer, mechanism or cryptographic key is absent
- **THEN** startup or identity initialization fails with a controlled non-secret configuration message

#### Scenario: Production selects unsafe mode or URI
- **WHEN** production uses mock mode or a non-HTTPS provider endpoint
- **THEN** configuration is rejected

### Requirement: Maintained JOSE support is minimal and centralized
The backend SHALL use one maintained JOSE/JWT dependency compatible with its Spring Boot version and centralize ID Perú JWT validation and flow-token signing/verification. It MUST NOT add an OAuth authorization server, broad security framework features, multiple competing JWT libraries or custom RSA parsing when the chosen library supports the requirement.

#### Scenario: Dependencies are reviewed
- **WHEN** the backend dependency tree is inspected
- **THEN** it contains only the minimal JOSE/security modules justified by provider and cookie-token validation

#### Scenario: Invalid algorithm is received
- **WHEN** a JWT advertises `none` or an unapproved algorithm
- **THEN** the centralized validator rejects it before claim access

### Requirement: Security filters protect only declared flow boundaries
The backend SHALL leave public only the endpoints required for initial query, ID Perú initiation/callback, health and environment-appropriate API documentation, while requiring the proper continuation or verified-flow purpose for identity state and future post-authentication APIs. Cookie-authenticated mutations SHALL also validate an allowed request origin.

#### Scenario: Public callback arrives from ID Perú
- **WHEN** the provider posts to the registered callback without a browser continuation cookie
- **THEN** the callback can resolve and validate the attempt by state without opening other protected APIs

#### Scenario: Cross-origin mutation uses a valid cookie
- **WHEN** a disallowed Origin submits a cookie-authenticated mutation
- **THEN** the request is rejected despite the cookie
