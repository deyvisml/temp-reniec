## MODIFIED Requirements

### Requirement: Clean authoritative schema from an empty database
Flyway SHALL create from an empty MySQL database a relational model composed of the existing six tables plus `cancellation_request_certificate` and `certificate_revocation_result`. `V1__create_cancellation_request_model.sql` MUST remain unchanged and an incremental V2 migration SHALL add the new structures so an existing V1 database is upgraded without deleting or rewriting its data. The schema SHALL contain no request-session table, selection-only table, status catalog, speculative cryptographic columns, generated guards, stored procedures, or complex triggers and SHALL match the JPA mappings validated by Hibernate.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway applies V1 followed by V2, creates all eight justified tables, and Hibernate validates the schema successfully

#### Scenario: Existing V1 database is upgraded
- **WHEN** Flyway runs against a database at V1 containing existing cancellation requests and related history
- **THEN** V2 adds the certificate and individual-result structures while preserving every existing row and leaving the new tables empty until used

#### Scenario: Migration history is inspected
- **WHEN** a contributor reviews the Flyway files
- **THEN** V1 remains byte-for-byte unchanged and the new schema is introduced only through a reproducible forward migration

### Requirement: Backend-controlled request states
The request status SHALL remain a backend-controlled string and SHALL support `NO_CERTIFICATES_AVAILABLE`, `CERTIFICATES_AVAILABLE`, `PENDING_IDENTITY_VERIFICATION`, `AUTHENTICATED_PENDING_SELECTION`, `CERTIFICATES_SELECTED`, `REVOCATION_IN_PROGRESS`, `REVOCATION_SUCCEEDED`, `REVOCATION_PARTIAL`, `REVOCATION_FAILED`, and `REVOCATION_OUTCOME_UNKNOWN` in addition to legacy values required by existing rows and the still-unmodified eligibility flow. The final outcome SHALL support successful, partial, failed, and uncertain revocation. Adding or retaining these values MUST NOT create a status catalog table or another current-step column.

#### Scenario: Lookup returns no certificates
- **WHEN** a future consultation completes with an empty certificate list
- **THEN** the request can represent `NO_CERTIFICATES_AVAILABLE` without creating certificate rows

#### Scenario: Authenticated citizen has certificates
- **WHEN** identity verification succeeds for a request with available certificates
- **THEN** the request can represent `AUTHENTICATED_PENDING_SELECTION` and later `CERTIFICATES_SELECTED`

#### Scenario: Individual results are aggregated
- **WHEN** a revocation operation reaches a successful, partial, failed, or uncertain overall result
- **THEN** the request status and final outcome can represent that result without reconstructing its current state from audit events

#### Scenario: Existing row contains a legacy state
- **WHEN** V2 is applied over a request written by the current eligibility implementation
- **THEN** its textual state remains readable and no automatic reinterpretation or destructive migration occurs

### Requirement: Repeatable eligibility checks
Each external consultation attempt SHALL remain stored in `certificate_eligibility_check` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, status, normalized availability result, optional external reference, request/response times, optional error code, correlation identifier, and creation time. A completed attempt MAY be the source of zero, one, or many `cancellation_request_certificate` rows, and every such row MUST be constrained to the same request as the attempt. Complete external payloads MUST NOT be stored. `(request_id, attempt_number)` SHALL remain unique, and attempt allocation SHALL occur while the parent request is locked.

#### Scenario: Consultation returns an empty list
- **WHEN** a completed attempt finds no current certificates
- **THEN** the attempt and request outcome are persisted with no certificate rows

#### Scenario: Consultation returns multiple certificates
- **WHEN** a completed attempt returns several current certificate issues
- **THEN** each issue can be persisted separately and references that attempt and its request

#### Scenario: Attempt from another request is assigned
- **WHEN** a certificate row references a consultation attempt belonging to another request
- **THEN** relational integrity rejects the row

#### Scenario: Attempt number is reused
- **WHEN** two eligibility rows use the same request and attempt number
- **THEN** the database rejects the duplicate

### Requirement: Idempotent revocation operations
Each technical revocation SHALL remain stored separately in `revocation_operation` using numeric identifiers, a numeric request foreign key, and a unique readable `idempotency_key`. It SHALL retain attempt number, status, optional external reference, lifecycle times, normalized overall result, optional error code, correlation identifier, and creation/update times. One operation MAY own multiple `certificate_revocation_result` rows, but only one result per certificate. The overall result MUST be derived from those rows and MUST NOT assume uniform success. The system MUST NOT create a new operation automatically after an uncertain outcome.

#### Scenario: Duplicate idempotency key is used
- **WHEN** a second operation is persisted with an existing idempotency key
- **THEN** the database rejects the duplicate

#### Scenario: Operation processes several certificates
- **WHEN** a future revocation submits multiple selected certificates
- **THEN** one operation relates to one individual result for each processed certificate

#### Scenario: Outcome is uncertain
- **WHEN** any individual result is uncertain
- **THEN** the same operation remains available for reconciliation, its overall outcome is uncertain, and no automatic duplicate is created

#### Scenario: Concurrent transition is attempted
- **WHEN** two transactions attempt an incompatible transition of the same operation or individual result
- **THEN** explicit operation locking, state-conditioned updates, or the individual result version permits only the valid transition

### Requirement: Explicit relational integrity without cascading deletion
All eight tables SHALL use numeric primary keys and matching numeric foreign keys. The database SHALL reject orphan rows, a certificate linked to a consultation from another request, and an individual result whose operation, certificate, request, or submitted UUID do not correspond. It SHALL retain request history by omitting cascading deletion. It SHALL keep focused uniqueness constraints for attempt numbers, request certificate UUIDs, idempotency keys, operation-certificate results, and receipt codes, plus only the indexes required by documented queries.

#### Scenario: Duplicate certificate is inserted
- **WHEN** the same canonical UUID is inserted twice for one request
- **THEN** the database rejects the second row

#### Scenario: Foreign certificate result is inserted
- **WHEN** an individual result combines an operation with a certificate from another request
- **THEN** the database rejects the row through composite foreign-key integrity

#### Scenario: Submitted UUID differs from certificate
- **WHEN** an individual result records a UUID other than the UUID of its referenced request certificate
- **THEN** the database rejects the row

#### Scenario: Request deletion is attempted with history
- **WHEN** a request with certificates or results is deleted directly
- **THEN** foreign-key integrity prevents accidental loss of the persisted evidence

### Requirement: Concrete repositories support minimum recovery queries
The backend SHALL retain one repository per entity requiring direct queries and SHALL add repositories for request certificates and individual revocation results. Queries SHALL support certificates by request, certificate by request and UUID, selected certificates, certificate count, latest consultation results, individual results by operation, result by operation and certificate, and counts grouped by individual status. Repositories MUST NOT introduce a generic custom base, empty service layer, bidirectional aggregate collection, eager history, or a separate selection repository.

#### Scenario: Selection is recovered
- **WHEN** a future use case resumes a request after identity verification
- **THEN** the repository returns its available certificates and clearly distinguishes the selected subset

#### Scenario: Operation result is reviewed
- **WHEN** a future use case loads a revocation operation
- **THEN** its individual results are returned without eagerly loading unrelated request history

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, incremental upgrade from V1 with existing rows, all eight tables, Hibernate validation, certificate cardinalities, selection combinations, optimistic concurrency, UUID uniqueness, operation-certificate uniqueness, cross-request rejection, submitted-UUID integrity, individual status persistence, partial aggregation, restricted deletion, and backend startup. Tests MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Clean persistence verification is run
- **WHEN** the integration suite starts MySQL from an empty database
- **THEN** V1 and V2 execute and all current and new entity mappings operate against the resulting schema

#### Scenario: Upgrade verification is run
- **WHEN** the suite migrates a V1 database containing representative existing data to V2
- **THEN** the data remains intact and the new tables and constraints are usable

#### Scenario: Selection matrix is tested
- **WHEN** requests with zero, one, and several certificates select one, several, or all available rows
- **THEN** persistence returns exactly the stored selection and enforces timestamp and version integrity

#### Scenario: Result matrix is tested
- **WHEN** successful, failed, and uncertain individual results are stored for an operation
- **THEN** duplicates and cross-request associations are rejected and the expected general outcome, including partial, is derivable

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the eight implemented tables with an updated entity-relationship diagram, a plain-language justification for each table, column descriptions, certificate and request states, indexes, integrity constraints, UUID treatment, selection strategy, individual-result strategy, overall-outcome derivation, concurrency, migration path, excluded sensitive data, and simple inspection queries. It MUST distinguish current implementation facts from still-pending external contracts.

#### Scenario: Contributor reviews the extended model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why certificate and individual-result rows exist, how selection is stored, how cross-request misuse is prevented, and how a partial outcome is calculated

### Requirement: Persistence redesign remains non-functional
This change MUST NOT change the home page or current initiation contract; consume a new certificate service; implement ID Perú, JWT, refresh tokens, selection UI, real or mock revocation execution, PDF or receipt generation, administrative modules, production infrastructure, or final retention policy. It MUST NOT add another database, Redis, event sourcing, CQRS, status catalogs, stored procedures, complex triggers, a selection-only table, cryptographic duplicate columns, or unrelated speculative fields.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** they contain only the incremental persistence model, states, repositories, tests, and documentation required for certificates and individual results, with no new citizen-facing behavior or external call

## ADDED Requirements

### Requirement: Consulted certificates belong to a cancellation request
The system SHALL persist each current certificate issue in `cancellation_request_certificate` with its cancellation request, source eligibility attempt, order number, emission creation time, canonical UUID, availability status, consultation time, selection flag, optional selection time, optimistic version, creation time, and update time. A request SHALL support zero, one, or many certificate rows. `(request_id, certificate_uuid)` MUST be unique.

#### Scenario: Request has no certificates
- **WHEN** the consultation returns an empty list
- **THEN** the request remains valid with zero certificate rows and can represent that continuation is blocked

#### Scenario: Request has one certificate
- **WHEN** one issue is returned
- **THEN** one certificate row preserves its order number, emission time, UUID, source attempt, availability, and consultation time

#### Scenario: Request has several certificates
- **WHEN** several distinct issues are returned
- **THEN** each is persisted as a separate row related to the same request

#### Scenario: UUID is duplicated within the request
- **WHEN** another row uses the same canonical UUID for that request
- **THEN** the database rejects it while permitting the same UUID in a different historical request

### Requirement: Certificate selection is stored without another table
Each request certificate SHALL store whether it is selected and when it became selected. The database SHALL require `selected_at` to be present exactly when `selected` is true. The entity SHALL use optimistic versioning for concurrent edits and SHALL allow repositories to distinguish selected and unselected rows. No table dedicated exclusively to selection SHALL exist.

#### Scenario: One certificate is selected
- **WHEN** one row among several is selected
- **THEN** only that row stores `selected=true` and a selection time

#### Scenario: Several or all certificates are selected
- **WHEN** the citizen selects several or every available row
- **THEN** every chosen row is independently queryable as selected and the others remain unselected

#### Scenario: Selection timestamp is inconsistent
- **WHEN** a row is stored as selected without a time or unselected with a selection time
- **THEN** entity or database validation rejects the inconsistent state

#### Scenario: Concurrent selection update occurs
- **WHEN** two transactions edit the same certificate from the same version
- **THEN** at most one update succeeds and the other receives an optimistic-lock conflict

### Requirement: Individual revocation result is persisted per certificate
The system SHALL persist each result in `certificate_revocation_result` with its request, revocation operation, request certificate, submitted UUID, controlled status, optional normalized code, message and external reference, optional processing time, correlation identifier, optimistic version, creation time, and update time. Status SHALL be one of `PENDING`, `SUCCEEDED`, `FAILED`, or `OUTCOME_UNKNOWN`. `(revocation_operation_id, request_certificate_id)` MUST be unique.

#### Scenario: Pending result is prepared
- **WHEN** a selected certificate is prepared for a future revocation call
- **THEN** one pending result preserves the exact submitted UUID and operation relationship

#### Scenario: Individual result completes
- **WHEN** the provider outcome is normalized as successful, failed, or uncertain
- **THEN** the existing row records the status, bounded metadata, processing time, and correlation without storing a complete provider payload

#### Scenario: Retry attempts duplicate a result
- **WHEN** a retry attempts to insert another result for the same operation and certificate
- **THEN** the database rejects the duplicate and the existing row remains the reconciliation target

### Requirement: Overall revocation outcome is derived from individual results
The backend SHALL derive the overall operation result from persisted individual statuses. No terminal result SHALL be produced while no results exist or any result is pending; any uncertain result SHALL yield `OUTCOME_UNKNOWN`; all successes SHALL yield `SUCCEEDED`; all failures SHALL yield `FAILED`; and a mix of successes and failures SHALL yield `PARTIAL`. The operation and request MAY store the derived value but MUST preserve the individual rows as its evidence.

#### Scenario: Every certificate succeeds
- **WHEN** all individual rows are `SUCCEEDED`
- **THEN** the derived overall result is `SUCCEEDED`

#### Scenario: Success and failure coexist
- **WHEN** at least one row succeeds and at least one row fails with none uncertain or pending
- **THEN** the derived overall result is `PARTIAL`

#### Scenario: An uncertain result exists
- **WHEN** one or more rows are `OUTCOME_UNKNOWN`
- **THEN** the derived overall result is `OUTCOME_UNKNOWN` even if other rows have known outcomes

#### Scenario: Results remain pending
- **WHEN** no rows exist or at least one row is `PENDING`
- **THEN** no terminal overall result is calculated

### Requirement: Certificate identifiers are protected without speculative cryptography
The UUID SHALL be stored once as canonical `CHAR(36)` ASCII data and SHALL be reused through relational references and the justified submitted-value snapshot. It MUST NOT be duplicated as ciphertext, hash, key-version, or public authorization token without a later institutional requirement. Full UUIDs MUST NOT appear in logs, URLs, technical endpoints, unauthenticated responses, or certificate lists shown before successful identity verification.

#### Scenario: Database is inspected by an authorized developer
- **WHEN** a request certificate row is queried directly for diagnosis
- **THEN** its canonical UUID is readable and no speculative cryptographic companion columns exist

#### Scenario: Unauthenticated response is produced
- **WHEN** the initial certificate consultation completes before identity verification
- **THEN** no certificate UUID or certificate-level data is exposed to the client

