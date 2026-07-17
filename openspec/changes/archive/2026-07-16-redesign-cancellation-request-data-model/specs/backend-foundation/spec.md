# Backend Foundation Specification Delta

## MODIFIED Requirements

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

### Requirement: Externalized local and test configuration
The backend SHALL provide common configuration plus distinct `local` and `test` profiles using Spring Boot configuration files. MySQL host, port, database name, user, and password SHALL be supplied or overridable through documented environment variables; the `test` profile SHALL receive disposable Testcontainers connection properties; local instructions SHALL activate `local`; and the repository MUST NOT contain production secrets or credentials. Hibernate SHALL validate rather than generate the schema, Flyway SHALL own schema changes, all persisted application dates SHALL use UTC semantics, and production configuration SHALL remain outside this change.

#### Scenario: Local profile is used
- **WHEN** a contributor starts the backend with `SPRING_PROFILES_ACTIVE=local` and valid documented MySQL environment values
- **THEN** common and local configuration are applied, Flyway validates or migrates the configured database, and the application starts

#### Scenario: Test profile is used
- **WHEN** the persistence integration suite starts the application
- **THEN** the `test` profile uses the disposable MySQL Testcontainer configuration and no manually installed MySQL or production-only resource is required

#### Scenario: Environment value overrides a default
- **WHEN** a documented environment variable such as `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT`, or `LOG_LEVEL_APP` is supplied
- **THEN** Spring Boot uses that value instead of any safe repository default

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

### Requirement: Fast isolated baseline tests
The backend SHALL retain fast tests that verify context startup, the health response, common validation-error formatting, generated correlation, valid correlation propagation, and invalid correlation replacement. Tests that do not exercise persistence SHALL remain isolated from MySQL and external services. Persistence behavior SHALL be verified separately with disposable MySQL Testcontainers so the complete verification command requires Java 21 and a compatible container runtime but no manually installed database or external functional service.

#### Scenario: Baseline test subset is executed
- **WHEN** a contributor runs the documented fast test subset
- **THEN** the technical web-foundation tests pass without a database, container, or external network service

#### Scenario: Complete verification is executed
- **WHEN** a contributor runs the documented Maven verification command with a compatible container runtime available
- **THEN** baseline tests and MySQL persistence integration tests pass without a manually configured database or functional integration

### Requirement: Concise local operation documentation
The backend SHALL contain a concise README covering prerequisites, Maven Wrapper commands to compile, test, package, verify, and run, the health URL, the `local` and `test` profiles, MySQL creation and configuration, every supported environment variable, Flyway schema ownership, persistence-test container requirements, and the procedure for recreating only a disposable local database after the initial migration replacement. It SHALL state the sensitive-data logging restrictions, prohibit automatic cleanup of databases containing relevant information, and note that production configuration is deferred.

#### Scenario: New contributor follows the README
- **WHEN** a contributor with Java 21, MySQL for local execution, and a compatible container runtime for persistence tests follows the backend README from a clean checkout
- **THEN** the contributor can configure the database, build, test, start the backend, and obtain a successful health response without undocumented infrastructure

#### Scenario: Contributor has the obsolete local V1 schema
- **WHEN** a contributor follows the documented migration-replacement note for a database containing only disposable development data
- **THEN** the contributor can recreate that local database and allow Flyway to build the redesigned schema from empty without using an undocumented repair command

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain the MySQL/Flyway/Testcontainers infrastructure and the seven-entity cancellation-request persistence model specified by `cancellation-request-persistence-model`. It MUST NOT create citizen-flow endpoints or use cases; functional JWT or refresh-token behavior; real ID Perú, certificate-lookup, revocation, document-storage, or other external integrations; complete progress recovery; PDF generation; production deployment; administrative modules; or citizen UI behavior. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, another database, Redis, workflow stored procedures, complex triggers, speculative interfaces, or unused layers.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** they contain the technical backend foundation, the explicitly required persistence model, tests, and documentation, with no functional citizen flow, external integration, or administrative capability
