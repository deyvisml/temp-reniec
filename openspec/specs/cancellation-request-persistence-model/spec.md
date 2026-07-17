# Cancellation Request Persistence Model Specification

## Purpose

Define the authoritative, simplified MySQL persistence model centered on the citizen's certificate cancellation request and its justified repeatable or independently managed records.

## Requirements

### Requirement: Clean authoritative schema from an empty database
Flyway SHALL create from an empty MySQL database a simplified relational model composed of `certificate_cancellation_request`, `certificate_eligibility_check`, `identity_verification`, `revocation_operation`, `cancellation_receipt`, and `cancellation_audit_event`. The schema SHALL contain no `cancellation_request_session`, `public_reference`, `consent_version`, `recoverable_until`, `expires_at`, optimistic-lock `version`, obsolete protected-value, or generated-guard structures and SHALL match the JPA mappings validated by Hibernate. An environment containing relevant information MUST use a separately designed forward migration instead of destructive cleanup.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway creates the six justified tables with their relationships and Hibernate validates the schema successfully

#### Scenario: Existing disposable local schema is upgraded
- **WHEN** a developer upgrades a local database containing only disposable development data
- **THEN** the documented procedure recreates the local database from the simplified authoritative migration history

#### Scenario: Relevant data is discovered
- **WHEN** implementation finds an environment whose current tables contain information that must be preserved
- **THEN** destructive consolidation stops and a forward-only migration is designed before that environment is changed

### Requirement: Cancellation request is the persistent root
The system SHALL persist each citizen journey in `certificate_cancellation_request` with a numeric auto-increment identifier, full DNI, current request status, current eligibility result, reason, optional OTHER description, confirmation time, final outcome, creation time, and update time. The request SHALL be the directly queryable source of truth for current progress, SHALL NOT duplicate a lifecycle classification or step derivable from its status, and MUST NOT contain a public UUID, consent-version, recovery-window, expiration-time, or optimistic-version column.

#### Scenario: Citizen request is created
- **WHEN** a new cancellation request is persisted
- **THEN** one readable request row represents its current citizen journey and related histories reference that row by numeric foreign key

#### Scenario: Current progress is inspected
- **WHEN** a contributor queries an unfinished request
- **THEN** `request_status` directly identifies the persisted progress without joining a session table or reading an additional current-step column

### Requirement: Backend-controlled request states
The request status SHALL be stored as a backend-controlled string supporting the confirmed journey states without a status catalog table or a separate lifecycle-status column. Eligibility SHALL retain a normalized current result on the request. Adding a future state SHALL require backend behavior and tests, but MUST NOT require a new table or column.

#### Scenario: Detailed status is stored
- **WHEN** a request advances through eligibility, identity, confirmation, revocation, completion, failure, or abandonment
- **THEN** its current status remains directly visible in the request row as a controlled textual value

#### Scenario: Revocation outcome is uncertain
- **WHEN** the latest revocation operation has an uncertain result
- **THEN** the request can represent that current status while the technical detail remains in `revocation_operation`

### Requirement: Reason and consent integrity
The request SHALL store a cancellation-reason code controlled by the backend from `THEFT`, `LOSS`, `DEVICE_OR_NUMBER_CHANGE`, `SUSPECTED_UNAUTHORIZED_USE`, and `OTHER`. `OTHER` SHALL store its description as readable text of at most 300 characters; other codes SHALL leave that field null. Confirmation SHALL require a reason and confirmation time. Once confirmed, application behavior MUST NOT modify the reason or description. A consent version MUST NOT be persisted until an institutionally versioned text and retention requirement are confirmed.

#### Scenario: OTHER reason is persisted
- **WHEN** a request selects `OTHER`
- **THEN** the request row stores a directly readable, length-limited description

#### Scenario: Confirmed reason is changed
- **WHEN** application behavior attempts to replace the reason of a confirmed request
- **THEN** the update is rejected before persistence

#### Scenario: Incomplete confirmation is attempted
- **WHEN** a request is confirmed without reason or confirmation time
- **THEN** entity validation or application behavior rejects the inconsistent state

### Requirement: Only one incompatible active request per DNI
The model SHALL permit historical requests for one DNI and SHALL provide an index supporting lookup by DNI, status, and recency. Progress recovery SHALL select the most recent unfinished request in a resumable status regardless of elapsed time. It MUST NOT create a session row, generated guard column, recovery-window column, expiration-time column, automatic `EXPIRED` transition, or complex trigger. The use case SHALL enforce create-or-recover transactionally with explicit database locking where concurrent access is demonstrated.

#### Scenario: Active request is recovered
- **WHEN** the citizen supplies a DNI that has an unfinished request in a resumable state, regardless of when it was created
- **THEN** the existing request and its current `request_status` are returned without creating a request session

#### Scenario: Historical request exists
- **WHEN** only completed, abandoned, or failed requests exist for a DNI
- **THEN** a later request for the same DNI can coexist and history remains queryable

#### Scenario: Concurrent active request is attempted
- **WHEN** concurrent initiation attempts arrive for the same DNI
- **THEN** transaction and explicit locking produce at most one incompatible active request without relying on an optimistic-version column

### Requirement: DNI and sensitive text are stored by purpose
The request SHALL store the citizen DNI once as `CHAR(8)` constrained to eight numeric digits and SHALL store an optional OTHER description as bounded readable text. It MUST NOT create DNI hash, ciphertext, key-version, last-four, encrypted-description, or generated-guard columns. It MUST NOT persist authentication tokens, credentials, biometric data, photographs, complete provider payloads, or document bytes. Full DNI values MUST NOT appear in application logs, error responses, technical endpoints, or URLs.

#### Scenario: Stored request data is inspected
- **WHEN** a contributor queries `certificate_cancellation_request`
- **THEN** one `dni` column clearly identifies the citizen and no duplicate protected representation exists

#### Scenario: Invalid DNI is inserted
- **WHEN** a value other than exactly eight numeric digits is stored as DNI
- **THEN** database or entity validation rejects it

### Requirement: Repeatable eligibility checks
Each external eligibility attempt SHALL be stored in `certificate_eligibility_check` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, status, normalized result including controlled `ERROR`, optional external reference, request/response times, optional error code, correlation identifier, and creation time. It MUST NOT store complete provider payloads. `(request_id, attempt_number)` SHALL be unique, and attempt allocation SHALL occur while the parent request is locked.

#### Scenario: Eligibility is retried
- **WHEN** an eligibility consultation is safely repeated for the same retryable request
- **THEN** a new row with the next attempt number preserves both results without duplicating the request data

#### Scenario: Attempt number is reused
- **WHEN** two eligibility rows use the same request and attempt number
- **THEN** the database rejects the duplicate

#### Scenario: Controlled technical error occurs
- **WHEN** the gateway reports a technical error
- **THEN** the attempt is finalized as failed with normalized `ERROR`, a controlled code and correlation, without storing a provider payload

### Requirement: Repeatable privacy-limited identity verification
Each ID PerÃº attempt SHALL be stored in `identity_verification` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, provider, status, optional external reference, DNI-match result, start/completion times, optional error or cancellation code, correlation identifier, and creation time. It MUST NOT store a verified-identity hash, biometric data, photographs, provider tokens, credentials, or complete provider responses. `(request_id, attempt_number)` SHALL be unique.

#### Scenario: Identity verification is repeated
- **WHEN** a citizen retries identity verification
- **THEN** a new simplified attempt row is related to the same request without sensitive provider payloads

#### Scenario: Verified identity is queried
- **WHEN** the latest successful verification is requested
- **THEN** the repository can locate it by request, status, and attempt number without a stored identity hash

### Requirement: Idempotent revocation operations
Each technical revocation SHALL be stored separately in `revocation_operation` using numeric identifiers, a numeric request foreign key, and a unique readable `idempotency_key`. It SHALL contain attempt number, status, optional external reference, preparation/submission/response/completion times, normalized result, optional error code, correlation identifier, and creation/update times. It MUST NOT contain an optimistic-version or generated open-operation guard and MUST NOT create a new operation automatically after an uncertain outcome. Future concurrent transitions SHALL lock or conditionally update the expected operation state.

#### Scenario: Duplicate idempotency key is used
- **WHEN** a second operation is persisted with an existing idempotency key
- **THEN** the database rejects the duplicate

#### Scenario: Outcome is unknown
- **WHEN** an operation has an uncertain result
- **THEN** the same operation remains available for reconciliation and no automatic duplicate is created

#### Scenario: Concurrent transition is attempted
- **WHEN** two transactions attempt an incompatible transition of the same revocation operation
- **THEN** an explicit lock or state-conditioned update permits only the valid transition without an optimistic-version column

### Requirement: Receipt is evidence independent of revocation execution
Each `cancellation_receipt` SHALL use numeric identifiers and foreign keys to its request and successful revocation operation. It SHALL store a unique receipt code, generation status, optional storage reference, generation/availability times, optional error code, and creation/update times. It MUST NOT store a PDF BLOB, document hash, or template version until a confirmed document contract requires them. Receipt failure MUST NOT change a confirmed revocation result.

#### Scenario: Receipt becomes available
- **WHEN** receipt generation succeeds for a successful revocation
- **THEN** its code, availability, and storage reference are queryable independently from the revocation result

#### Scenario: Receipt generation fails
- **WHEN** receipt generation fails after revocation succeeded
- **THEN** the receipt records failure and the request does not become failed solely for that reason

#### Scenario: Receipt code is duplicated
- **WHEN** two receipts use the same receipt code
- **THEN** the database rejects the duplicate

### Requirement: Append-only non-authoritative audit history
Relevant lifecycle events SHALL be appended to `cancellation_audit_event` using numeric identifiers and a numeric request foreign key. Each event SHALL contain event type, optional previous/new status, optional result, correlation identifier, origin, and occurrence time. It MUST NOT contain broad technical-detail fields or external payloads. Audit rows MUST NOT be replayed to reconstruct current request state.

#### Scenario: Request transition is audited
- **WHEN** a relevant transition is recorded
- **THEN** a compact immutable event identifies the request, transition, origin, correlation, and time

#### Scenario: Audit history is queried
- **WHEN** request history is requested
- **THEN** events are returned chronologically without replacing the request row as source of truth

### Requirement: Explicit relational integrity without cascading deletion
All six tables SHALL use numeric primary keys and matching numeric foreign keys. The database SHALL reject orphan rows and SHALL retain request history by omitting cascading deletion. It SHALL keep only meaningful uniqueness constraints for attempt numbers, idempotency keys, and receipt codes, and focused indexes for documented queries.

#### Scenario: Orphan child is inserted
- **WHEN** a child row references a missing request or revocation operation
- **THEN** the database rejects the insert

#### Scenario: Request deletion is attempted with history
- **WHEN** a request with related rows is deleted directly
- **THEN** foreign-key integrity prevents accidental history deletion

### Requirement: Concrete repositories support minimum recovery queries
The backend SHALL retain one repository for each of the six entities that has required direct queries. Repositories SHALL support request lookup by numeric identifier, unfinished and recent request lookup by DNI, latest eligibility and identity attempts, current revocation, available receipt, and ordered audit history. Recovery MUST use the request repository without a time cutoff and MUST NOT introduce a session repository, expiration query, generic custom base repository, empty service layer, bidirectional collection, or eager history.

#### Scenario: Active request is recovered
- **WHEN** the repository is queried with a DNI and resumable statuses
- **THEN** it returns the matching unfinished request using the direct DNI and request-status columns without evaluating elapsed time

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, all six tables, absence of the removed table and columns, readable DNI persistence, entity relationships, creation and update, explicit concurrent access behavior, attempt uniqueness, revocation idempotency, receipt-code uniqueness, ordered audit, current-state recovery by DNI regardless of elapsed time, basic integrity, and backend startup. Tests MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Persistence verification is run
- **WHEN** a contributor runs the documented backend verification command with a container runtime available
- **THEN** the six-table relational schema and repository behavior are verified against real MySQL semantics

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the six implemented tables with a compact entity-relationship diagram, a plain-language justification for each table, column descriptions, controlled states, indexes, integrity constraints, direct progress-recovery strategy, the explicit plaintext-DNI decision, excluded sensitive data, and simple inspection queries. It MUST clearly distinguish the citizen request from technical revocation and MUST NOT describe request sessions, public UUID references, consent versions, recovery windows, optimistic versions, or removed cryptographic infrastructure as current behavior.

#### Scenario: Contributor reviews the model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why every remaining table and column exists and how progress is recovered from the request row

### Requirement: Persistence redesign remains non-functional
This change MUST NOT add real ID PerÃº integration, functional JWT, refresh tokens, session persistence, complete progress-recovery UI, real revocation calls, PDF generation, administrative modules, production infrastructure, final retention policy, or invented external contracts. It MUST NOT add another database, Redis, event sourcing, CQRS, status catalogs, stored procedures, complex triggers, generated guard columns, replacement session tables, or additional speculative columns.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** the change contains the six simplified persistence responsibilities and contract adaptations with no new citizen-flow stage, session infrastructure, or external integration

