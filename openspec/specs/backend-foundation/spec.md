
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
The backend SHALL use Spring Boot's managed web MVC, validation, Actuator, Spring Data JPA, Flyway MySQL, and MySQL driver support plus the minimum Spring Boot and MySQL Testcontainers test support required by the implemented foundation and cancellation-request persistence model. It MUST NOT add security, messaging, cache, alternate database, external-integration, OpenAPI, distributed-tracing, cryptographic, document-generation, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for HTTP serving, validation, health, MySQL persistence and migrations, or the specified tests and no out-of-scope starter is present

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
The backend SHALL contain a concise README covering prerequisites, one-time creation of ignored `backend/.env` from the committed local example, the distinction between default host port `3307` and MySQL container port `3306`, adjustment of `DB_PORT` when another local conflict exists, Docker Compose configuration validation, commands to start, inspect, stop, and destructively reset the local MySQL 8.4 service, Maven Wrapper commands to compile, test, package, verify, and run, the health URL, the `local` and `test` profiles, every supported environment variable, Flyway schema ownership, persistence-test container requirements, and the procedure for recreating only a disposable local database. It SHALL state the sensitive-data logging restrictions, prohibit automatic cleanup of databases containing relevant information, distinguish local example credentials from production configuration, and note that production configuration is deferred.

#### Scenario: New contributor uses Compose alongside native MySQL
- **WHEN** a contributor with native MySQL reserved on `3306`, Java 21, and a compatible Docker Compose runtime follows the backend README from a clean checkout
- **THEN** the contributor can create the private local environment file once, validate and start Compose on `3307`, build and test the backend, start it with the local profile, and obtain a successful health response without stopping native MySQL or repeatedly assigning database variables

#### Scenario: Contributor resolves another port collision
- **WHEN** the default host port `3307` is unavailable
- **THEN** the README directs the contributor to select a free `DB_PORT` in the ignored `.env` and verify the resolved host-to-container mapping before starting the backend

#### Scenario: Contributor stops local MySQL without losing data
- **WHEN** a contributor follows the normal stop procedure after changing the published port
- **THEN** the MySQL container is removed while its named volume and migrated data are preserved

#### Scenario: Contributor resets a disposable local database
- **WHEN** a contributor confirms that the local database contains no relevant information and follows the separately documented destructive reset procedure
- **THEN** only the local Compose volume is removed and Flyway can build the redesigned schema from empty on the next backend startup without an undocumented repair command

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain the MySQL/Flyway/Testcontainers infrastructure, the seven-entity cancellation-request persistence model specified by `cancellation-request-persistence-model`, and a development-only Docker Compose service containing MySQL 8.4. It MUST NOT containerize backend or frontend, add application Dockerfiles or PowerShell startup scripts, or introduce citizen-flow endpoints or use cases; functional JWT or refresh-token behavior; real ID Perú, certificate-lookup, revocation, document-storage, or other external integrations; complete progress recovery; PDF generation; production deployment; administrative modules; or citizen UI behavior. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, another database, Redis, workflow stored procedures, complex triggers, speculative interfaces, or unused layers.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, Compose model, and runtime routes are inspected
- **THEN** they contain the technical backend foundation, the explicitly required persistence model, a single local MySQL service, tests, and documentation, with no application container, functional citizen flow, external integration, production deployment configuration, or administrative capability
