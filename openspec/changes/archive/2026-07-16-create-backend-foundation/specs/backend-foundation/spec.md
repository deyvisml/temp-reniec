## ADDED Requirements

### Requirement: Executable single-module backend project
The repository SHALL contain a single-module Maven project at `/backend` using Java 21, Spring Boot 4.1.0, executable JAR packaging, Maven Wrapper 3.9.16, and the institutional base package `pe.gob.reniec.certificados.cancelacion`. The project SHALL compile and start without a database or external service.

#### Scenario: Backend is built from a clean checkout
- **WHEN** a contributor with Java 21 runs the documented Maven Wrapper build from `/backend`
- **THEN** Maven compiles the application and produces an executable Spring Boot JAR without requiring MySQL or external services

#### Scenario: Backend starts locally
- **WHEN** a contributor starts the packaged application or uses the documented Spring Boot Maven command
- **THEN** the Spring application context and embedded web server start successfully

### Requirement: Minimal managed dependencies
The backend SHALL use only Spring Boot's managed `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, and the minimum test-scoped Spring Boot test support required by this change. It MUST NOT add database, security, messaging, external integration, OpenAPI, distributed tracing, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for web serving, validation, health, or the specified tests and no out-of-scope starter is present

### Requirement: Simple feature-oriented structure
The backend SHALL place the application class at the root of the institutional package and SHALL create only packages and classes used by the current technical foundation. Shared error and web concerns MAY be grouped under focused packages, while empty cancellation, domain, application, persistence, integration, or API layers MUST NOT be created.

#### Scenario: Initial package tree is inspected
- **WHEN** a contributor reviews the production source tree
- **THEN** every package contains a component used by this change and no speculative architecture or empty functional module exists

### Requirement: Externalized local and test configuration
The backend SHALL provide common configuration plus distinct `local` and `test` profiles using Spring Boot configuration files. Environment-dependent values SHALL be overridable through environment variables, tests SHALL activate `test`, local instructions SHALL activate `local`, and the repository MUST NOT contain secrets or credentials. Production configuration SHALL remain outside this change.

#### Scenario: Local profile is used
- **WHEN** a contributor starts the backend with `SPRING_PROFILES_ACTIVE=local`
- **THEN** common and local configuration are applied and the application starts without a secret, database, or external dependency

#### Scenario: Test profile is used
- **WHEN** the automated suite starts the application
- **THEN** the `test` profile is active and no local or production-only resource is required

#### Scenario: Environment value overrides a default
- **WHEN** a documented environment variable such as `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT`, or `LOG_LEVEL_APP` is supplied
- **THEN** Spring Boot uses that value instead of the safe repository default

### Requirement: Minimal operational health endpoint
The running backend SHALL expose Spring Boot Actuator health at `/actuator/health`, SHALL report that the application is operational when the context and server are available, and SHALL expose no other Actuator endpoint over HTTP. Health MUST NOT depend on MySQL or an external service and MUST NOT reveal internal details to the client.

#### Scenario: Operational application responds to health check
- **WHEN** a client sends `GET /actuator/health` to a running backend
- **THEN** the server returns HTTP 200 with Actuator status `UP`

#### Scenario: Actuator exposure is limited
- **WHEN** the management endpoint configuration is inspected or a non-health Actuator endpoint is requested
- **THEN** only `health` is exposed over HTTP and internal health details are not returned

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
The backend SHALL include automated tests that verify context startup, the health response, common validation-error formatting, generated correlation, valid correlation propagation, and invalid correlation replacement. The tests SHALL run with the `test` profile on an isolated random port and MUST NOT use MySQL, containers, external network services, or functional integration mocks.

#### Scenario: Baseline test suite is executed
- **WHEN** a contributor runs the documented Maven Wrapper test or verification command
- **THEN** all baseline tests pass without infrastructure beyond Java 21

### Requirement: Concise local operation documentation
The backend SHALL contain a concise README covering prerequisites, Maven Wrapper commands to compile, test, package, and run, the health URL, the `local` and `test` profiles, and every supported environment variable with its safe default or purpose. It SHALL also state the sensitive-data logging restrictions and that production configuration is deferred.

#### Scenario: New contributor follows the README
- **WHEN** a contributor with Java 21 follows the backend README from a clean checkout
- **THEN** the contributor can build, test, start the local backend, and obtain a successful health response without undocumented infrastructure

### Requirement: Technical-foundation-only boundary
The change MUST NOT create `/frontend`; configure MySQL, migrations, a data model, JWT, refresh tokens, ID Perú, certificate lookup, DNI business validation, cancellation reasons, confirmation, revocation, receipts, external-service mocks, functional auditing, production deployment, Docker, administrative modules, or citizen-flow behavior. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, speculative interfaces, or unused layers.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** they contain only the backend technical foundation, tests, and local documentation described by this capability, with no functional or architectural additions outside scope
