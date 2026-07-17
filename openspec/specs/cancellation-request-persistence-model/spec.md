
# Cancellation Request Persistence Model Specification

## Purpose

Define the authoritative MySQL persistence model centered on the citizen's certificate cancellation request, including its repeatable technical attempts, sessions, revocation, receipt, audit history, protection boundaries, integrity rules, and minimum recovery queries.

## Requirements

### Requirement: Clean authoritative schema from an empty database
Flyway SHALL create the complete cancellation-request persistence model from an empty MySQL database. The authoritative initial schema SHALL replace the obsolete `cancellation_process` and `cancellation_session` schema when no relevant data exists, SHALL contain no obsolete table or column, and SHALL match the JPA mappings validated by Hibernate. An environment containing relevant information MUST use a separately designed forward migration instead of destructive cleanup.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway creates all seven required domain tables and Hibernate validates the resulting schema successfully

#### Scenario: Existing disposable local schema uses the old V1
- **WHEN** a developer upgrades a local database containing only disposable SPEC-04 data
- **THEN** the documented procedure requires recreating that local database before applying the new authoritative V1

#### Scenario: Relevant data is discovered
- **WHEN** implementation finds an environment whose old tables contain information that must be preserved
- **THEN** destructive replacement stops and a forward-only preservation migration is designed before that environment is changed

### Requirement: Cancellation request is the persistent root
The system SHALL persist each citizen journey in `certificate_cancellation_request`, identified by an application-generated UUID and containing its current request status, lifecycle status, eligibility result, protected DNI representations, reason, protected OTHER description, consent version, confirmation timestamp, final outcome, recovery deadline, expiration timestamp, creation timestamp, update timestamp, and optimistic-lock version. The request snapshot SHALL be the source of truth and MUST NOT require audit replay. A request MUST NOT represent an administrative approval case.

#### Scenario: Citizen request is created
- **WHEN** a new cancellation request is persisted
- **THEN** one request row represents the complete citizen journey and carries its directly queryable current state

#### Scenario: Request state changes concurrently
- **WHEN** two transactions update the same stored request version
- **THEN** the first committed update advances the version and the stale update is rejected by optimistic locking

### Requirement: Backend-controlled request states
The request status SHALL be stored as a backend-controlled string supporting at least `STARTED`, `CHECKING_ELIGIBILITY`, `NOT_ELIGIBLE`, `ELIGIBLE`, `PENDING_IDENTITY_VERIFICATION`, `IDENTITY_VERIFIED`, `REASON_REGISTERED`, `PENDING_CONFIRMATION`, `CONFIRMED`, `REVOCATION_IN_PROGRESS`, `COMPLETED`, `FAILED`, `OUTCOME_UNKNOWN`, `RECEIPT_AVAILABLE`, `EXPIRED`, and `ABANDONED`. The schema SHALL also store the stable lifecycle classification `ACTIVE`, `FINALIZED`, `ABANDONED`, or `EXPIRED`. The system MUST NOT create a status catalog table.

#### Scenario: Detailed status is stored
- **WHEN** a request advances through a supported lifecycle state
- **THEN** its current status is stored directly as a controlled textual value without creating a row in a catalog table

#### Scenario: Revocation outcome is uncertain
- **WHEN** a request has an unresolved technical revocation outcome
- **THEN** its detailed status can be `OUTCOME_UNKNOWN` while its lifecycle remains active for uniqueness and recovery purposes

### Requirement: Reason and consent integrity
The request SHALL store a cancellation-reason code controlled by the backend from `THEFT`, `LOSS`, `DEVICE_OR_NUMBER_CHANGE`, `SUSPECTED_UNAUTHORIZED_USE`, and `OTHER`. Confirmation SHALL require a stored reason, consent-text version, and confirmation time. `OTHER` SHALL require a protected description and protection-key version, while other reason codes SHALL NOT retain an OTHER description. Once a request is confirmed, persistence operations MUST NOT modify its reason or protected reason description.

#### Scenario: OTHER reason is persisted
- **WHEN** a request selects `OTHER`
- **THEN** the request stores protected description bytes and their key version without a plaintext description

#### Scenario: Confirmed reason is changed
- **WHEN** code attempts to modify the reason of a confirmed request
- **THEN** the persistence model rejects the change

#### Scenario: Incomplete confirmation is attempted
- **WHEN** a request is marked confirmed without its reason, consent-text version, or confirmation time
- **THEN** the model rejects the inconsistent state

### Requirement: Only one incompatible active request per DNI
The schema SHALL permit historical requests for one citizen but SHALL atomically prevent more than one request with lifecycle `ACTIVE` for the same secure DNI lookup hash. The implementation SHALL use a MySQL-compatible generated nullable guard and unique index or an equivalently atomic strategy, rather than a check-then-insert race in application code.

#### Scenario: Historical request exists
- **WHEN** a finalized, abandoned, or expired request exists for a DNI lookup hash
- **THEN** another request for that hash may be created as active

#### Scenario: Concurrent active request is attempted
- **WHEN** a second active request is inserted for a DNI hash that already has an active request
- **THEN** the database rejects the conflicting insert with a uniqueness violation

### Requirement: DNI and sensitive text are stored by purpose
The request SHALL distinguish a deterministic keyed DNI hash for controlled lookup, encrypted DNI bytes for authorized recovery, a protection-key version, and only the last four DNI digits for presentation. It SHALL store the OTHER-description only in protected form. It MUST NOT persist a full plaintext DNI, plaintext sensitive description, secret, biometric, provider payload, or authentication token. The persistence layer SHALL accept already-protected values and MUST remain decoupled from the future institutional cryptographic implementation.

#### Scenario: Stored request data is inspected
- **WHEN** a request row and its JPA mapping are reviewed
- **THEN** lookup, recoverable, and presentation forms are separate and no plaintext fallback column exists

#### Scenario: Protected value lacks key metadata
- **WHEN** encrypted DNI or protected OTHER-description bytes are supplied without their required key version
- **THEN** persistence rejects the incomplete protected-value pair

### Requirement: Repeatable eligibility checks
`certificate_eligibility_check` SHALL record each eligibility consultation with a request reference, request-local attempt number, consultation status, normalized result, optional external reference, request and response timestamps, technical error code, correlation identifier, and creation timestamp. Normalized results SHALL support `ELIGIBLE`, `NOT_ELIGIBLE`, `UNAVAILABLE`, and `INCONCLUSIVE`. Attempt numbers SHALL be unique within a request, and complete provider responses MUST NOT be stored.

#### Scenario: Eligibility is retried
- **WHEN** a controlled retry is recorded for one request
- **THEN** a new attempt row is stored and earlier eligibility attempts remain unchanged

#### Scenario: Attempt number is reused
- **WHEN** two eligibility records use the same request and attempt number
- **THEN** the database rejects the duplicate

### Requirement: Repeatable privacy-limited identity verification
`identity_verification` SHALL record each verification with a request reference, request-local attempt number, provider, state, optional external reference, secure verified-identity reference or hash, normalized DNI-match result, start and completion timestamps, error or cancellation code, correlation identifier, and creation timestamp. States SHALL support `STARTED`, `VERIFIED`, `REJECTED`, `CANCELLED`, `IDENTITY_MISMATCH`, and `ERROR`. Attempt numbers SHALL be unique within a request. The model MUST NOT contain biometric data, photographs, ID Perú tokens, complete provider responses, or unnecessary personal data.

#### Scenario: Identity verification is repeated
- **WHEN** a citizen makes another permitted identity-verification attempt
- **THEN** the attempt is recorded separately and prior verification history is preserved

#### Scenario: Verified identity is queried
- **WHEN** the latest valid identity verification for a request is requested
- **THEN** the repository returns the most recent matching `VERIFIED` record without loading unrelated attempt collections

### Requirement: Multiple secure request sessions
`cancellation_request_session` SHALL associate multiple sessions with a request and store only a unique hash of the session or refresh-token reference, a token-family identifier, creation time, expiry time, last-use time, invalidation time and reason, optional non-invasive client reference, and update time. Raw tokens MUST NOT be stored and client references MUST NOT use invasive fingerprinting.

#### Scenario: Progress is re-established on another device
- **WHEN** a future recovery use case creates a new session after renewed identity verification
- **THEN** the request can retain the earlier session and associate the new independently expiring session

#### Scenario: Session is invalidated
- **WHEN** a session is invalidated
- **THEN** its invalidation time and reason are persisted without deleting the request or other sessions

### Requirement: Idempotent revocation operations
`revocation_operation` SHALL represent a technical revocation execution separately from the citizen request. It SHALL store a globally unique idempotency key, request-local technical attempt number, state, optional external reference, preparation, submission, response and completion times, normalized result, technical error code, next status-check time, correlation identifier, timestamps, and optimistic-lock version. States SHALL support `PREPARED`, `SUBMITTED`, `SUCCEEDED`, `FAILED`, and `OUTCOME_UNKNOWN`.

#### Scenario: Duplicate idempotency key is used
- **WHEN** another operation is persisted with an existing idempotency key
- **THEN** the database rejects the duplicate regardless of request

#### Scenario: Outcome is unknown
- **WHEN** an operation has `OUTCOME_UNKNOWN`
- **THEN** it remains the open operation for that request and a new operation cannot be created automatically to bypass reconciliation

#### Scenario: Technical retry is permitted after resolution
- **WHEN** domain rules later permit a new technical attempt after the prior operation is conclusively resolved
- **THEN** a new request-local attempt number and new idempotency key can be stored while preserving the prior operation

### Requirement: Receipt is evidence independent of revocation execution
`cancellation_receipt` SHALL reference both its cancellation request and a `SUCCEEDED` revocation operation belonging to that request. It SHALL store a unique receipt code, generation state, external storage reference, document hash, template version, generation and availability times, technical error code, and timestamps. States SHALL support `PENDING`, `GENERATING`, `AVAILABLE`, and `FAILED`. The PDF or other document bytes MUST NOT be stored in MySQL.

#### Scenario: Receipt becomes available
- **WHEN** receipt generation succeeds after confirmed revocation
- **THEN** its external storage reference, document hash, template version, and availability time are stored and queryable

#### Scenario: Receipt generation fails
- **WHEN** a receipt enters `FAILED` after the revocation succeeded
- **THEN** the receipt records its failure without changing the request's confirmed revocation result to failed

#### Scenario: Receipt code is duplicated
- **WHEN** another receipt is stored with an existing receipt code
- **THEN** the database rejects the duplicate

### Requirement: Append-only non-authoritative audit history
`cancellation_audit_event` SHALL append relevant request lifecycle events with event type, previous and new status, normalized result, correlation identifier, optional external reference, sanitized minimal technical information, origin, and event time. Application persistence behavior MUST NOT update or delete audit events. Audit history SHALL NOT be used to reconstruct the request's current state and SHALL NOT implement event sourcing.

#### Scenario: Request transition is audited
- **WHEN** a relevant lifecycle transition is recorded
- **THEN** a new immutable audit row is appended while the current request state remains directly available on the request

#### Scenario: Audit history is queried
- **WHEN** audit events for a request are requested
- **THEN** they are returned in chronological order without automatically loading them through the request entity

### Requirement: Explicit relational integrity without cascading deletion
All tables SHALL use UUID primary keys, required foreign keys, appropriate uniqueness constraints, indexes aligned to required lookups, coherent UTC temporal fields, and database checks for enforceable date and field-pair invariants. Eligibility, identity, session, revocation, receipt, and audit rows SHALL belong to exactly one request; each receipt SHALL additionally reference one revocation. Foreign keys MUST NOT cascade deletion while retention rules remain unconfirmed.

#### Scenario: Orphan child is inserted
- **WHEN** a child row references a nonexistent request or receipt references a nonexistent operation
- **THEN** the database rejects the row through a foreign-key constraint

#### Scenario: Request deletion is attempted with history
- **WHEN** a request with dependent history is deleted directly
- **THEN** the database does not silently cascade-delete its sessions, attempts, operations, receipts, or audit events

### Requirement: Concrete repositories support minimum recovery queries
The backend SHALL provide one repository per persisted entity because each has required entity-specific access. Collectively they SHALL support request lookup by identifier, active request by secure DNI hash, most recent request by secure DNI hash, expiration candidates, active sessions, latest eligibility check, latest valid identity verification, current/open revocation operation, available receipt, and ordered audit history. The model MUST NOT introduce a generic custom base repository, an empty service layer, bidirectional aggregate collections, or eager loading of large histories.

#### Scenario: Active request is recovered
- **WHEN** the repository is queried with a secure DNI hash
- **THEN** it returns at most the one request classified as active

#### Scenario: Expired candidates are selected
- **WHEN** requests eligible for expiration are queried at a UTC cutoff
- **THEN** active requests whose expiration criteria are met are returned without scanning child histories

### Requirement: Integration tests exercise MySQL behavior
The persistence test suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, root creation and updates, optimistic concurrency, all repeatable records, multiple sessions, reason and consent integrity, unique active requests, revocation idempotency, receipt uniqueness, audit history, foreign keys, current-state queries, expiration and abandonment behavior, and backend startup against the clean schema. Tests MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Persistence verification is run
- **WHEN** a contributor runs the documented backend verification command with a container runtime available
- **THEN** the redesigned schema and repository behavior are verified against real MySQL semantics

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the implemented schema with an entity-relationship diagram, entity descriptions, relationships, states, sensitive fields, indexes, integrity constraints, idempotency strategy, progress-recovery strategy, and external-contract dependencies. It SHALL state that the cancellation request is the complete citizen procedure and revocation is a technical operation caused by its confirmation.

#### Scenario: Contributor reviews the model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can distinguish request, revocation, and receipt responsibilities and trace every documented table and relation to the migration and JPA model

### Requirement: Persistence redesign remains non-functional
This change MUST NOT add citizen-flow endpoints, real certificate consultation, real ID Perú integration, functional JWT issuance, complete cross-device recovery, real revocation calls, PDF generation, administrative modules, production infrastructure, final retention policy, or invented external contracts. It MUST NOT add another database, Redis, event sourcing, CQRS, stored procedures for workflow logic, or complex triggers.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** the change contains persistence structure, queries, tests, and documentation only, with no functional citizen journey or external integration

