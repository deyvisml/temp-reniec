## ADDED Requirements

### Requirement: Local MySQL Compose service
The repository SHALL provide `backend/compose.yaml` with exactly one application service using the `mysql:8.4` image. The service SHALL initialize its database and users from environment variables, publish the configured local port to container port `3306`, and MUST NOT contain the backend, frontend, migration runner, or any unrelated service.

#### Scenario: Compose model is inspected
- **WHEN** a contributor runs `docker compose --env-file .env.example config` from `/backend`
- **THEN** the configuration is valid and resolves to a single MySQL 8.4 service with no application container

### Requirement: Shared local environment template
The backend SHALL provide a committed `.env.example` containing only explicitly local values for `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `MYSQL_ROOT_PASSWORD`. A developer-created `backend/.env` SHALL be ignored by Git, SHALL be automatically usable by Docker Compose, and SHALL be optionally importable by the Spring Boot `local` profile without adding a dotenv library. Process environment variables SHALL remain able to override imported file values, and repository values MUST NOT represent production credentials.

#### Scenario: Contributor performs one-time local setup
- **WHEN** a contributor copies `backend/.env.example` to `backend/.env` and runs commands from `/backend`
- **THEN** Compose and Spring Boot use the same database coordinates and credentials without requiring the contributor to export them in every terminal session

#### Scenario: Private environment file is absent
- **WHEN** `backend/.env` does not exist but the required database values are supplied as process environment variables
- **THEN** the optional import does not fail configuration loading and the `local` profile uses the supplied values

#### Scenario: Git exclusions are inspected
- **WHEN** repository ignore rules are evaluated for `backend/.env` and `backend/.env.example`
- **THEN** the private `.env` is ignored and the example remains eligible for version control

### Requirement: Persistent and healthy local database
The MySQL service SHALL store database files in a named Docker volume and SHALL expose a MySQL-native healthcheck. Normal stop and removal commands SHALL preserve the volume, while deletion of local database data SHALL require a separate explicit volume-removal command.

#### Scenario: Database is restarted normally
- **WHEN** a contributor stops the environment with `docker compose down` and starts it again
- **THEN** the named volume is reused and previously migrated local data remains available

#### Scenario: Compose waits for MySQL readiness
- **WHEN** a contributor runs the documented detached start command with health waiting enabled
- **THEN** Compose reports successful startup only after the MySQL healthcheck passes or reports a failure if it does not become healthy

#### Scenario: Disposable database is reset explicitly
- **WHEN** a contributor confirms the data is local and disposable and runs the documented destructive reset command
- **THEN** the named volume is removed and the next start initializes an empty MySQL database

### Requirement: Spring-managed local migrations
Starting the backend with the `local` profile against the Compose database SHALL cause the existing Flyway integration to validate or migrate the database before the application becomes operational. No separate migration container, manual SQL execution, Hibernate schema generation, or alternate schema mechanism SHALL be introduced.

#### Scenario: Empty local database is used
- **WHEN** MySQL is healthy with an empty configured database and Spring Boot starts with the `local` profile
- **THEN** Flyway applies the committed migrations, Hibernate validates the resulting schema, and the backend starts

#### Scenario: Migration cannot complete
- **WHEN** Flyway cannot validate or migrate the configured local database
- **THEN** backend startup fails clearly without silently repairing, cleaning, or replacing the schema

### Requirement: Concise local Compose workflow
Backend documentation SHALL describe prerequisites, one-time `.env` creation, Compose configuration validation, database start, status and log inspection, normal stop, destructive reset, backend startup with `local`, and health verification. It SHALL distinguish the persistent stop from destructive reset, identify all example credentials as local-only, and MUST NOT require a PowerShell script, Dockerfile, or application container.

#### Scenario: Contributor follows the quick local workflow
- **WHEN** a contributor with Java 21 and a compatible Docker Compose runtime follows the README from a clean checkout
- **THEN** the contributor can validate Compose, start MySQL, start Spring Boot, observe Flyway completion, and receive `UP` from `/actuator/health` without manually creating MySQL objects or repeatedly assigning database variables

#### Scenario: Scope of local infrastructure is reviewed
- **WHEN** the change files and documentation are inspected
- **THEN** they contain no PowerShell script, application Dockerfile, backend or frontend container, production credential, deployment configuration, or citizen-flow behavior
