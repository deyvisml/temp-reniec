## MODIFIED Requirements

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
