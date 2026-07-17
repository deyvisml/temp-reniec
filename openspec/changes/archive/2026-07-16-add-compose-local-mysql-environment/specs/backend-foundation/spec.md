## MODIFIED Requirements

### Requirement: Externalized local and test configuration
The backend SHALL provide common configuration plus distinct `local` and `test` profiles using Spring Boot configuration files. The `local` profile SHALL optionally import `backend/.env` as a properties-formatted Config Data resource so a committed local template can be copied once and shared with Docker Compose; process environment values SHALL remain able to override imported values. MySQL host, port, database name, user, and password SHALL be supplied or overridable through documented environment variables; the `test` profile SHALL receive disposable Testcontainers connection properties; local instructions SHALL activate `local`; and `backend/.env`, production secrets, and production credentials MUST NOT be committed. Hibernate SHALL validate rather than generate the schema, Flyway SHALL own schema changes, all persisted application dates SHALL use UTC semantics, and production configuration SHALL remain outside this change.

#### Scenario: Local profile uses the private environment file
- **WHEN** a contributor starts the backend from `/backend` with the `local` profile and a `.env` copied from the documented local template
- **THEN** common and local configuration are applied, the database values are loaded without per-session assignments, Flyway validates or migrates the configured database, and the application starts

#### Scenario: Local environment value overrides the file
- **WHEN** a documented process environment variable such as `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `APP_NAME`, `LOG_LEVEL_ROOT`, or `LOG_LEVEL_APP` is supplied while `.env` is present
- **THEN** Spring Boot uses the process environment value instead of the imported file value or any safe repository default

#### Scenario: Optional local file is not used
- **WHEN** `.env` is absent and valid documented MySQL environment values are supplied directly to the process
- **THEN** the optional import does not block startup and the local profile uses the supplied configuration

#### Scenario: Test profile is used
- **WHEN** the persistence integration suite starts the application
- **THEN** the `test` profile uses the disposable MySQL Testcontainer configuration and no manually installed MySQL, local `.env`, Compose database, or production-only resource is required

#### Scenario: Schema ownership is inspected
- **WHEN** persistence configuration is reviewed
- **THEN** Flyway is the schema owner and Hibernate is limited to validating that JPA mappings match the migrated schema

### Requirement: Concise local operation documentation
The backend SHALL contain a concise README covering prerequisites, one-time creation of ignored `backend/.env` from the committed local example, Docker Compose configuration validation, commands to start, inspect, stop, and destructively reset the local MySQL 8.4 service, Maven Wrapper commands to compile, test, package, verify, and run, the health URL, the `local` and `test` profiles, every supported environment variable, Flyway schema ownership, persistence-test container requirements, and the procedure for recreating only a disposable local database. It SHALL state the sensitive-data logging restrictions, prohibit automatic cleanup of databases containing relevant information, distinguish local example credentials from production configuration, and note that production configuration is deferred.

#### Scenario: New contributor follows the README
- **WHEN** a contributor with Java 21 and a compatible Docker Compose runtime follows the backend README from a clean checkout
- **THEN** the contributor can create the private local environment file once, validate and start MySQL, build and test the backend, start it with the local profile, and obtain a successful health response without manually installing MySQL, creating database objects, or repeatedly assigning database variables

#### Scenario: Contributor stops local MySQL without losing data
- **WHEN** a contributor follows the normal stop procedure
- **THEN** the MySQL container is removed while its named volume and migrated data are preserved

#### Scenario: Contributor resets a disposable local database
- **WHEN** a contributor confirms that the local database contains no relevant information and follows the separately documented destructive reset procedure
- **THEN** only the local Compose volume is removed and Flyway can build the redesigned schema from empty on the next backend startup without an undocumented repair command

### Requirement: Technical-foundation-only boundary
The backend foundation MAY contain the MySQL/Flyway/Testcontainers infrastructure, the seven-entity cancellation-request persistence model specified by `cancellation-request-persistence-model`, and a development-only Docker Compose service containing MySQL 8.4. It MUST NOT containerize backend or frontend, add application Dockerfiles or PowerShell startup scripts, or introduce citizen-flow endpoints or use cases; functional JWT or refresh-token behavior; real ID Perú, certificate-lookup, revocation, document-storage, or other external integrations; complete progress recovery; PDF generation; production deployment; administrative modules; or citizen UI behavior. It MUST NOT introduce microservices, queues, event sourcing, CQRS, a multi-module Maven build, another database, Redis, workflow stored procedures, complex triggers, speculative interfaces, or unused layers.

#### Scenario: Completed change is reviewed for scope
- **WHEN** the implementation diff, Compose model, and runtime routes are inspected
- **THEN** they contain the technical backend foundation, the explicitly required persistence model, a single local MySQL service, tests, and documentation, with no application container, functional citizen flow, external integration, production deployment configuration, or administrative capability
