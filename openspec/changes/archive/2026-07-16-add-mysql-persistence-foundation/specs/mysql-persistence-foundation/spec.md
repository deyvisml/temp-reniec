## ADDED Requirements

### Requirement: MySQL persistence dependencies and profiles
The backend SHALL use Spring Data JPA, Flyway with MySQL support, and the official MySQL Connector/J using versions managed by Spring Boot 4.1.0. Normal local execution SHALL use MySQL as the only application database and SHALL obtain host, port, database name, username, and password from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`; the repository MUST NOT contain database credentials.

#### Scenario: Local datasource is configured externally
- **WHEN** a contributor activates the `local` profile and supplies the documented database variables
- **THEN** the backend connects to that MySQL database without a credential stored in a tracked configuration file

#### Scenario: Required credentials are absent
- **WHEN** normal local execution starts without the required username or password
- **THEN** startup fails rather than silently using a shared or embedded credential

### Requirement: Flyway-owned reproducible schema
Flyway SHALL be the only mechanism that creates or changes the application schema. A versioned migration SHALL create the initial persistence schema from an empty MySQL database, SHALL contain no credentials or unnecessary seed data, and SHALL be validated at startup; Hibernate schema generation MUST be disabled in favor of validation and Flyway clean MUST be disabled.

#### Scenario: Empty database is migrated
- **WHEN** the complete backend starts against an empty compatible MySQL database
- **THEN** Flyway creates its history plus the process and session tables and JPA validates the resulting schema

#### Scenario: Migration history is invalid
- **WHEN** the applied migration history does not match the packaged migrations
- **THEN** startup fails before the application serves citizen requests

### Requirement: Minimal cancellation process table
The schema SHALL contain one `cancellation_process` table with an internal UUID identifier, `dni_reference_hash`, `dni_last_four`, current `status`, `active`, `created_at`, `updated_at`, `expires_at`, and numeric `version`. It MUST NOT contain reason, confirmation, revocation, receipt, ID Perú, certificate-detail, external-response, file, or per-screen fields.

#### Scenario: Process is persisted and recovered
- **WHEN** a valid process entity is saved and its internal identifier is queried
- **THEN** the same safe DNI reference, partial digits, state, activity, timestamps, expiry, and version are recovered

#### Scenario: Process schema is reviewed
- **WHEN** the initial migration and entity are inspected
- **THEN** all process information is concentrated in the single table and no table or column anticipates a future citizen screen or integration

### Requirement: Extensible backend-controlled process states
The backend SHALL store process state as a string controlled by a Java enum with exactly the initial values `STARTED`, `ELIGIBLE`, `NOT_ELIGIBLE`, `PENDING_IDENTITY_VERIFICATION`, `EXPIRED`, and `ABANDONED`. Eligibility SHALL be represented by `ELIGIBLE` or `NOT_ELIGIBLE` rather than a duplicate result column. The database MUST NOT use a state catalogue table or an enumerating constraint that requires structural schema change to add a future state.

#### Scenario: Eligibility outcome is stored
- **WHEN** a test process changes from `STARTED` to `ELIGIBLE` or `NOT_ELIGIBLE`
- **THEN** the state itself records the minimum eligibility outcome and no separate eligibility field is written

#### Scenario: Terminal state updates activity
- **WHEN** a process changes to `NOT_ELIGIBLE`, `EXPIRED`, or `ABANDONED` through its entity behavior
- **THEN** the persisted process is inactive without implementing a complete future transition matrix

### Requirement: Safe DNI persistence boundary
The persistence model MUST NOT contain a full plaintext DNI, use a DNI as primary key, or expose a DNI as a public identifier. `dni_reference_hash` SHALL accept only an opaque 64-character hexadecimal reference prepared for controlled lookup, and `dni_last_four` SHALL contain only four numeric presentation digits. This change MUST NOT derive the reference with an unkeyed DNI hash or add recoverable ciphertext before an institutional cryptographic mechanism exists.

#### Scenario: Safe reference is persisted
- **WHEN** a process is created with a synthetic opaque reference and four display digits
- **THEN** persistence stores those values without any full DNI value

#### Scenario: Unsafe reference shape is rejected
- **WHEN** a process supplies a short, non-hexadecimal, or otherwise invalid lookup reference
- **THEN** validation prevents that process from being persisted

### Requirement: Justified one-to-many session persistence
The schema SHALL contain one `cancellation_session` table because a process can have multiple sessions with independent lifetimes. Each session SHALL contain an internal UUID, a required process foreign key, a unique irreversible `session_reference_hash`, `created_at`, `expires_at`, and optional `invalidated_at`; it MUST NOT contain JWTs, refresh tokens, cookies, credentials, or reversible session secrets.

#### Scenario: Multiple sessions belong to one process
- **WHEN** two sessions with different synthetic reference hashes are saved for one process
- **THEN** both rows reference that process and retain independent expiry and invalidation values

#### Scenario: Session is invalidated
- **WHEN** a persisted session is invalidated
- **THEN** `invalidated_at` is stored without deleting the process or implying JWT revocation behavior

### Requirement: Focused repositories and current queries
The backend SHALL provide only concrete Spring Data repositories needed for process and session persistence. The process repository SHALL support save, lookup by internal identifier, optimistic state updates through a loaded entity, and lookup of the most recently updated active non-expired process by safe DNI reference; the session repository SHALL support save and lookup by unique safe session reference. The change MUST NOT create custom generic repositories, empty services, ports, adapters, controllers, or speculative persistence interfaces.

#### Scenario: Current process is found by safe reference
- **WHEN** active non-expired, expired, and inactive processes exist for the same synthetic DNI reference
- **THEN** the current-process query returns only the most recently updated active non-expired process

#### Scenario: Session is found by safe reference
- **WHEN** a session has been saved with a unique synthetic session reference
- **THEN** the session repository recovers it without receiving a raw token

### Requirement: UTC timestamps and basic relational integrity
Process creation and update timestamps SHALL be maintained automatically in UTC, expiry SHALL be later than creation, and the session foreign key, uniqueness, non-null values, lengths, and temporal ordering SHALL be enforced with JPA validation and appropriate database constraints. The schema SHALL add only indexes that support the process-current lookup, session foreign key, and unique session-reference lookup.

#### Scenario: Update timestamp advances
- **WHEN** a persisted process is modified in a later transaction
- **THEN** `updated_at` advances while `created_at` remains unchanged

#### Scenario: Invalid session relation is rejected
- **WHEN** a session references a process that does not exist or reuses an existing session reference
- **THEN** MySQL rejects the write through a foreign-key or uniqueness constraint

### Requirement: Optimistic process concurrency
The process entity SHALL use JPA optimistic locking mapped to its numeric `version`. State changes MUST load and save the entity without a bulk update that bypasses version checking; this change MUST NOT add pessimistic locks or distributed coordination.

#### Scenario: Concurrent process update conflicts
- **WHEN** two persistence contexts load the same process version and both attempt to commit a state change
- **THEN** the second conflicting update fails with an optimistic-locking exception and does not overwrite the first

### Requirement: Database-aware safe health behavior
The normally configured application SHALL include the standard datasource contributor in Actuator's aggregate health while still exposing only `/actuator/health` with details hidden. A database loss after startup SHALL make health non-successful without exposing JDBC URLs, usernames, SQL, stack traces, or credentials; an unavailable database or invalid migration during startup SHALL prevent a misleading successful start.

#### Scenario: Database is healthy
- **WHEN** the complete backend is running with a reachable migrated MySQL database
- **THEN** `GET /actuator/health` returns the aggregate `UP` response without component details

#### Scenario: Database becomes unavailable
- **WHEN** the running application can no longer validate its datasource
- **THEN** the health endpoint reports a non-healthy aggregate status without sensitive connection detail

### Requirement: MySQL integration tests without manual database
Persistence tests SHALL run against a clean MySQL Testcontainer and MUST NOT use H2, an installed developer database, external services, or real personal data. They SHALL verify migrations, full application startup, process creation and recovery, state update, current-process filtering, sessions, basic integrity, timestamps, and optimistic concurrency.

#### Scenario: Persistence suite runs on a clean machine
- **WHEN** a contributor with Java 21 and a compatible container runtime runs the documented persistence verification command
- **THEN** Testcontainers provisions MySQL, Flyway migrates it, all persistence tests pass, and the container is disposed without manual database preparation

### Requirement: Concise database operation documentation
The backend README SHALL document the supported MySQL baseline, local database and least-privilege user preparation, all five database variables, Flyway's automatic startup behavior, backend execution, persistence tests, and a safe local reset procedure. It SHALL distinguish destructive local reset from shared environments and state that production backup, retention, encryption, and deployment remain deferred.

#### Scenario: Contributor prepares local persistence
- **WHEN** a contributor follows the README with a compatible MySQL instance
- **THEN** the contributor can create the local database/user, configure variables, run migrations through application startup, execute tests, and inspect health without an undocumented step or committed secret

### Requirement: Persistence-foundation-only boundary
This change MUST NOT implement certificate lookup, functional DNI validation, JWT, refresh tokens, cookies, progress recovery, cross-device recovery, ID Perú, cancellation reasons, confirmation, revocation, receipts, functional auditing, external integrations, administrative modules, stored procedures, complex triggers, Redis, file storage, replication, high availability, or production backup. It MUST NOT modify `/frontend` or create tables per screen, state, or integration.

#### Scenario: Completed persistence change is reviewed
- **WHEN** dependencies, migrations, source packages, routes, and tables are inspected
- **THEN** they contain only the MySQL persistence foundation, two justified tables, tests, and documentation with no citizen-flow behavior or anticipatory architecture
