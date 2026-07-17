## MODIFIED Requirements

### Requirement: Executable single-module backend project
The repository SHALL contain a single-module Maven project at `/backend` using Java 21, Spring Boot 4.1.0, executable JAR packaging, Maven Wrapper 3.9.16, and the institutional base package `pe.gob.reniec.certificados.cancelacion`. The project SHALL compile without infrastructure and the normally configured application SHALL start when its MySQL database is reachable and validly migrated, without requiring any external citizen service.

#### Scenario: Backend is built from a clean checkout
- **WHEN** a contributor with Java 21 runs the documented Maven Wrapper build phase that does not execute persistence integration tests from `/backend`
- **THEN** Maven compiles the application and produces an executable Spring Boot JAR without requiring a manually installed MySQL database or external service

#### Scenario: Backend starts locally
- **WHEN** a contributor supplies the documented local MySQL variables and starts the packaged application or uses the documented Spring Boot Maven command
- **THEN** Flyway validates or migrates the schema and the Spring application context and embedded web server start successfully

### Requirement: Minimal managed dependencies
The backend SHALL use Spring Boot's managed `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`, Flyway core and MySQL support, official MySQL Connector/J, and the minimum Spring Boot/Testcontainers test support required for MySQL integration. It MUST NOT add security, messaging, external integration, OpenAPI, distributed tracing, another database, or other preventive dependencies.

#### Scenario: Dependency set is reviewed
- **WHEN** the Maven dependency declarations are inspected
- **THEN** every direct dependency is required for web serving, validation, health, MySQL persistence, migrations, or the specified tests and no out-of-scope starter is present

### Requirement: Externalized local and test configuration
The backend SHALL provide common configuration plus distinct `local` and `test` profiles using Spring Boot configuration files. Environment-dependent values SHALL be overridable through environment variables, normal local execution SHALL require externally supplied database credentials, tests SHALL activate `test`, and the repository MUST NOT contain secrets or credentials. Production configuration SHALL remain outside this change.

#### Scenario: Local profile is used
- **WHEN** a contributor starts the backend with `SPRING_PROFILES_ACTIVE=local` and supplies the documented MySQL variables
- **THEN** common and local configuration are applied and the application starts without an external citizen service or repository-stored secret

#### Scenario: Test profile is used
- **WHEN** the persistence suite starts the complete application
- **THEN** the `test` profile is active and database connection values come from its ephemeral MySQL container rather than a local or production resource

#### Scenario: Environment value overrides a default
- **WHEN** a documented environment variable such as `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT`, `LOG_LEVEL_APP`, `DB_HOST`, `DB_PORT`, or `DB_NAME` is supplied
- **THEN** Spring Boot uses that value instead of the safe repository default while database credentials remain externally required

### Requirement: Minimal operational health endpoint
The running backend SHALL expose Spring Boot Actuator health at `/actuator/health`, SHALL include the configured datasource in its aggregate operational status during normal execution, and SHALL expose no other Actuator endpoint over HTTP. Health details MUST remain hidden and MUST NOT reveal connection data, credentials, SQL, migration details, or other internals.

#### Scenario: Operational application responds to health check
- **WHEN** a client sends `GET /actuator/health` to a running backend with a healthy MySQL connection
- **THEN** the server returns HTTP 200 with aggregate Actuator status `UP` and no component detail

#### Scenario: Actuator exposure is limited
- **WHEN** the management endpoint configuration is inspected or a non-health Actuator endpoint is requested
- **THEN** only `health` is exposed over HTTP and internal health and datasource details are not returned

### Requirement: Fast isolated baseline tests
The backend SHALL retain automated isolated tests for correlation, safe errors, validation, and web behavior that can explicitly run without datasource/JPA/Flyway auto-configuration, and SHALL add separate MySQL Testcontainers integration coverage for the complete application and persistence. Neither group MUST use an installed MySQL database, external network service, functional integration mock, or real personal data.

#### Scenario: Baseline test suite is executed
- **WHEN** a contributor runs the documented fast-test command with Java 21
- **THEN** the isolated web and unit tests pass without a database or container

#### Scenario: Complete verification suite is executed
- **WHEN** a contributor runs the documented persistence verification command with a compatible container runtime
- **THEN** the complete application and persistence tests pass against an ephemeral MySQL database without manual database configuration

### Requirement: Concise local operation documentation
The backend SHALL contain a concise README covering Java and container prerequisites, Maven Wrapper commands to compile, run fast tests, run persistence verification, package and start, the health URL, the `local` and `test` profiles, MySQL/Flyway preparation, and every supported environment variable with its safe default or purpose. It SHALL state the sensitive-data logging restrictions and that production credentials, backup, retention, encryption, and deployment are deferred.

#### Scenario: New contributor follows the README
- **WHEN** a contributor with Java 21 and compatible MySQL follows the backend README from a clean checkout
- **THEN** the contributor can build, test, migrate, start the local backend, and obtain a successful health response without undocumented infrastructure or repository-stored credentials

### Requirement: Technical-foundation-only boundary
The backend foundation MAY include the MySQL process/session persistence defined by `mysql-persistence-foundation`, but MUST NOT create or modify `/frontend`; implement JWT, refresh tokens, ID Perú, certificate lookup, functional DNI validation, cancellation reasons, confirmation, revocation, receipts, external-service mocks, functional auditing, progress recovery, production deployment, or administrative modules. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, speculative interfaces, unused layers, or tables per screen, state, or integration.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, runtime routes, dependencies, and schema are inspected
- **THEN** they contain only the backend technical and MySQL persistence foundations, tests, and local documentation described by their capabilities, with no functional or architectural additions outside scope
