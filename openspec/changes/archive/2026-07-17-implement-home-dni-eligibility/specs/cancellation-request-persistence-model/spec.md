## MODIFIED Requirements

### Requirement: Only one incompatible active request per DNI
The model SHALL permit historical requests for one DNI and SHALL provide an index supporting lookup by DNI, status, and recency. It MUST NOT use generated guard columns or complex triggers. The initiation use case SHALL define `STARTED`, `CHECKING_ELIGIBILITY`, `ELIGIBLE`, and `PENDING_IDENTITY_VERIFICATION` as incompatible active states for this stage and SHALL enforce create-or-recover transactionally with MySQL locking, expiration handling, uniqueness checks, and concurrency tests.

#### Scenario: Historical request exists
- **WHEN** a completed or terminal request exists for a DNI and the citizen explicitly initiates a later request
- **THEN** the later request can coexist and history remains queryable

#### Scenario: Concurrent active request is attempted
- **WHEN** concurrent initiation attempts arrive for the same DNI
- **THEN** the transaction and locking strategy creates at most one active request and either recovers it or returns a controlled in-progress conflict to the other caller

#### Scenario: Active request is eligible
- **WHEN** an unexpired request for the DNI is already `ELIGIBLE` or `PENDING_IDENTITY_VERIFICATION`
- **THEN** the use case recovers it without creating another request or eligibility attempt

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

### Requirement: Persistence redesign remains non-functional
The persistence model SHALL now support the citizen eligibility entry endpoint, request public reference, active-request recovery, and eligibility-attempt lifecycle specified by `citizen-eligibility-entry`. It MUST NOT add real certificate consultation, real ID Perú integration, functional JWT, complete cross-device recovery, reason or confirmation processing, real revocation calls, PDF generation, administrative modules, production infrastructure, final retention policy, or invented external contracts. It MUST NOT add another database, Redis, event sourcing, CQRS, status catalogs, stored procedures, complex triggers, generated guard columns, or additional speculative tables.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff, migration, persistence model and runtime routes are inspected
- **THEN** the change uses the seven simplified persistence responsibilities plus one justified public-reference column for the initial citizen eligibility journey, with no later flow stage or real external integration

## ADDED Requirements

### Requirement: Request public reference is persistent and unique
`certificate_cancellation_request` SHALL store an application-generated UUID public reference in a non-null unique column. Flyway SHALL populate it for pre-existing rows before enforcing the constraint. Numeric primary keys SHALL remain internal and the public reference MUST NOT be treated as authentication.

#### Scenario: Existing schema is migrated
- **WHEN** Flyway applies the migration to a database containing cancellation requests
- **THEN** every existing and new request has a distinct public reference and all existing functional data remains intact

#### Scenario: Duplicate public reference is persisted
- **WHEN** two requests are assigned the same public reference
- **THEN** the database rejects the duplicate
