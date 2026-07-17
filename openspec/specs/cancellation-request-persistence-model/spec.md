# Cancellation Request Persistence Model Specification

## Purpose

Define the authoritative, simplified MySQL persistence model centered on the citizen's certificate cancellation request and its justified repeatable or independently managed records.

## Requirements

### Requirement: Clean authoritative schema from an empty database
Flyway SHALL create from an empty MySQL database a simplified relational model composed of `certificate_cancellation_request`, `certificate_eligibility_check`, `identity_verification`, `cancellation_request_session`, `revocation_operation`, `cancellation_receipt`, and `cancellation_audit_event`. The schema SHALL contain no obsolete protected-value or generated-guard columns and SHALL match the JPA mappings validated by Hibernate. An environment containing relevant information MUST use a separately designed forward migration instead of destructive cleanup.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway creates the seven justified tables with their relationships and Hibernate validates the schema successfully

#### Scenario: Existing disposable local schema uses the complex V1
- **WHEN** a developer upgrades a local database containing only disposable development data
- **THEN** the documented procedure requires recreating that local database before applying the simplified authoritative V1

#### Scenario: Relevant data is discovered
- **WHEN** implementation finds an environment whose current tables contain information that must be preserved
- **THEN** destructive replacement stops and a forward-only preservation migration is designed before that environment is changed

### Requirement: Cancellation request is the persistent root
The system SHALL persist each citizen journey in `certificate_cancellation_request` with a numeric auto-increment identifier, full DNI, current request status, current eligibility result, reason, optional OTHER description, consent version, confirmation time, final outcome, recovery and expiration times, creation time, update time, and optimistic-lock version. The request SHALL be the directly queryable source of truth for current progress, SHALL NOT duplicate a lifecycle classification derivable from its status, and MUST NOT represent an administrative approval case.

#### Scenario: Citizen request is created
- **WHEN** a new cancellation request is persisted
- **THEN** one readable request row represents its current citizen journey and related histories reference that row by numeric foreign key

#### Scenario: Request state changes concurrently
- **WHEN** two transactions update the same stored request version
- **THEN** the first committed update advances the version and the stale update is rejected by optimistic locking

### Requirement: Backend-controlled request states
The request status SHALL be stored as a backend-controlled string supporting the confirmed journey states without a status catalog table or a separate lifecycle-status column. Eligibility SHALL retain a normalized current result on the request. Adding a future state SHALL require backend behavior and tests, but MUST NOT require a new table or column.

#### Scenario: Detailed status is stored
- **WHEN** a request advances through eligibility, identity, confirmation, revocation, completion, expiration, or abandonment
- **THEN** its current status remains directly visible in the request row as a controlled textual value

#### Scenario: Revocation outcome is uncertain
- **WHEN** the latest revocation operation has an uncertain result
- **THEN** the request can represent that current status while the technical detail remains in `revocation_operation`

### Requirement: Reason and consent integrity
The request SHALL store a cancellation-reason code controlled by the backend from `THEFT`, `LOSS`, `DEVICE_OR_NUMBER_CHANGE`, `SUSPECTED_UNAUTHORIZED_USE`, and `OTHER`. `OTHER` SHALL store its description as readable text of at most 300 characters; other codes SHALL leave that field null. Confirmation SHALL require a reason, consent version, and confirmation time. Once confirmed, application behavior MUST NOT modify the reason or description.

#### Scenario: OTHER reason is persisted
- **WHEN** a request selects `OTHER`
- **THEN** the request row stores a directly readable, length-limited description

#### Scenario: Confirmed reason is changed
- **WHEN** application behavior attempts to replace the reason of a confirmed request
- **THEN** the update is rejected before persistence

#### Scenario: Incomplete confirmation is attempted
- **WHEN** a request is confirmed without reason, consent version, or confirmation time
- **THEN** entity validation or application behavior rejects the inconsistent state

### Requirement: Only one incompatible active request per DNI
The model SHALL permit historical requests for one DNI and SHALL provide an index supporting lookup by DNI, status, and recency. It MUST NOT use generated guard columns or complex triggers. When request creation is implemented, the use case SHALL define the incompatible active states and enforce the rule transactionally with concurrency tests.

#### Scenario: Historical request exists
- **WHEN** a completed request exists for a DNI
- **THEN** a later request for the same DNI can coexist and history remains queryable

#### Scenario: Concurrent active request is attempted
- **WHEN** the future creation use case receives concurrent attempts for the same DNI
- **THEN** its transaction and locking strategy, rather than a speculative generated column, determines whether one attempt is rejected or the existing request is recovered

### Requirement: DNI and sensitive text are stored by purpose
The request SHALL store the citizen DNI once as `CHAR(8)` constrained to eight numeric digits and SHALL store an optional OTHER description as bounded readable text. It MUST NOT create DNI hash, ciphertext, key-version, last-four, encrypted-description, or generated-guard columns. It MUST NOT persist authentication tokens, credentials, biometric data, photographs, complete provider payloads, or document bytes. Full DNI values MUST NOT appear in application logs, error responses, technical endpoints, or URLs.

#### Scenario: Stored request data is inspected
- **WHEN** a contributor queries `certificate_cancellation_request`
- **THEN** one `dni` column clearly identifies the citizen and no duplicate protected representation exists

#### Scenario: Invalid DNI is inserted
- **WHEN** a value other than exactly eight numeric digits is stored as DNI
- **THEN** database or entity validation rejects it

### Requirement: Repeatable eligibility checks
Each external eligibility attempt SHALL be stored in `certificate_eligibility_check` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, status, normalized result, optional external reference, request/response times, optional error code, correlation identifier, and creation time. It MUST NOT store complete provider payloads. `(request_id, attempt_number)` SHALL be unique.

#### Scenario: Eligibility is retried
- **WHEN** an eligibility consultation is repeated for the same request
- **THEN** a new row with the next attempt number preserves both results without duplicating the request data

#### Scenario: Attempt number is reused
- **WHEN** two eligibility rows use the same request and attempt number
- **THEN** the database rejects the duplicate

### Requirement: Repeatable privacy-limited identity verification
Each ID Perú attempt SHALL be stored in `identity_verification` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, provider, status, optional external reference, DNI-match result, start/completion times, optional error or cancellation code, correlation identifier, and creation time. It MUST NOT store a verified-identity hash, biometric data, photographs, provider tokens, credentials, or complete provider responses. `(request_id, attempt_number)` SHALL be unique.

#### Scenario: Identity verification is repeated
- **WHEN** a citizen retries identity verification
- **THEN** a new simplified attempt row is related to the same request without sensitive provider payloads

#### Scenario: Verified identity is queried
- **WHEN** the latest successful verification is requested
- **THEN** the repository can locate it by request, status, and attempt number without a stored identity hash

### Requirement: Multiple secure request sessions
The model SHALL allow multiple `cancellation_request_session` rows per request using numeric identifiers and a unique opaque `session_reference` that MUST NOT authenticate by itself. Each row SHALL contain creation, expiration, optional last-use, optional invalidation, invalidation reason, and update times. It MUST NOT store JWT, refresh tokens, token-family identifiers, credentials, or invasive device fingerprints. Token hashes SHALL be added only by a future JWT design if required.

#### Scenario: Progress is re-established on another device
- **WHEN** a citizen later verifies identity and recovery is implemented
- **THEN** a new session row can reference the existing request without deleting prior session history

#### Scenario: Session is invalidated
- **WHEN** a session is invalidated
- **THEN** its invalidation time and reason remain queryable and the session no longer qualifies as active

### Requirement: Idempotent revocation operations
Each technical revocation SHALL be stored separately in `revocation_operation` using numeric identifiers, a numeric request foreign key, and a unique readable `idempotency_key`. It SHALL contain attempt number, status, optional external reference, preparation/submission/response/completion times, normalized result, optional error code, correlation identifier, creation/update times, and optimistic-lock version. It MUST NOT use a generated open-operation guard or create a new operation automatically after an uncertain outcome.

#### Scenario: Duplicate idempotency key is used
- **WHEN** a second operation is persisted with an existing idempotency key
- **THEN** the database rejects the duplicate

#### Scenario: Outcome is unknown
- **WHEN** an operation has an uncertain result
- **THEN** the same operation remains available for reconciliation and no automatic duplicate is created

#### Scenario: Technical retry is permitted after resolution
- **WHEN** a future integration authorizes a new technical attempt after resolving the previous one
- **THEN** a new row uses the next unique attempt number and a new idempotency key

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
All seven tables SHALL use numeric primary keys and matching numeric foreign keys. The database SHALL reject orphan rows and SHALL retain request history by omitting cascading deletion. It SHALL keep only meaningful uniqueness constraints for attempt numbers, session references, idempotency keys, and receipt codes, and focused indexes for documented queries.

#### Scenario: Orphan child is inserted
- **WHEN** a child row references a missing request or revocation operation
- **THEN** the database rejects the insert

#### Scenario: Request deletion is attempted with history
- **WHEN** a request with related rows is deleted directly
- **THEN** foreign-key integrity prevents accidental history deletion

### Requirement: Concrete repositories support minimum recovery queries
The backend SHALL retain one repository for each of the seven entities because each has required direct queries. Repositories SHALL support request lookup by numeric identifier, active and recent request lookup by DNI, active sessions, latest eligibility and identity attempts, current revocation, available receipt, ordered audit history, and expiration candidates. They MUST NOT introduce a generic custom base repository, empty service layers, bidirectional collections, or eager histories.

#### Scenario: Active request is recovered
- **WHEN** the repository is queried with a DNI and active statuses
- **THEN** it returns the matching current request using the direct DNI column

#### Scenario: Expired candidates are selected
- **WHEN** requests eligible for expiration are queried at a UTC cutoff
- **THEN** matching current request rows are returned through a focused index

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, all seven tables, readable DNI persistence, entity relationships, creation and update, optimistic concurrency, attempt uniqueness, session association, revocation idempotency, receipt-code uniqueness, ordered audit, current-state queries, expiration behavior, basic integrity, and backend startup. Tests MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Persistence verification is run
- **WHEN** a contributor runs the documented backend verification command with a container runtime available
- **THEN** the simplified relational schema and repository behavior are verified against real MySQL semantics

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the seven implemented tables with a compact entity-relationship diagram, a plain-language justification for each table, column descriptions, controlled states, indexes, integrity constraints, the explicit plaintext-DNI decision, excluded sensitive data, and simple inspection queries. It MUST clearly distinguish the citizen request from technical revocation and MUST NOT describe removed cryptographic or generated-column infrastructure as current behavior.

#### Scenario: Contributor reviews the model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why every table exists, what every column means, and how to locate a request and its related records

### Requirement: Persistence redesign remains non-functional
This change MUST NOT add citizen-flow endpoints, real certificate consultation, real ID Perú integration, functional JWT, complete cross-device recovery, real revocation calls, PDF generation, administrative modules, production infrastructure, final retention policy, or invented external contracts. It MUST NOT add another database, Redis, event sourcing, CQRS, status catalogs, stored procedures, complex triggers, generated guard columns, or additional speculative tables.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** the change contains the seven simplified persistence responsibilities, tests, and documentation with no functional citizen journey or external integration
