# Cancellation Request Persistence Model Specification

## Purpose

Define the authoritative, simplified MySQL persistence model centered on the citizen's certificate cancellation request and its justified repeatable or independently managed records.

> **Implementation alignment notice (SPEC-08):** These requirements describe the currently implemented schema. The current domain authority at `docs/context/PROJECT_CONTEXT.md` now requires request-linked certificate issues, selected certificates, and individual revocation results. Those structures are not implemented by this documentation-only change and require a later persistence change; this specification MUST NOT be treated as the target domain model where it conflicts with the updated context.
## Requirements
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

### Requirement: Cancellation request is the persistent root
The system SHALL persist each citizen journey in `certificate_cancellation_request` with a numeric auto-increment identifier, full DNI, current request status, current eligibility result, reason, optional OTHER description, confirmation time, final outcome, creation time, and update time. The request SHALL be the directly queryable source of truth for current progress, SHALL NOT duplicate a lifecycle classification or step derivable from its status, and MUST NOT contain a public UUID, consent-version, recovery-window, expiration-time, or optimistic-version column.

#### Scenario: Citizen request is created
- **WHEN** a new cancellation request is persisted
- **THEN** one readable request row represents its current citizen journey and related histories reference that row by numeric foreign key

#### Scenario: Current progress is inspected
- **WHEN** a contributor queries an unfinished request
- **THEN** `request_status` directly identifies the persisted progress without joining a session table or reading an additional current-step column

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
The model SHALL permit multiple historical requests for one DNI and SHALL provide an index supporting lookup by DNI, status, and recency. A new initiation SHALL never recover or return an earlier request as current progress. The use case SHALL serialize the decision by DNI, abandon a replaceable pre-confirmation request before creating a new request, and block initiation while a confirmed, in-progress, or uncertain operation could make another cancellation unsafe. It MUST NOT create a session row, recovery-window column, expiration-time column, generated guard column, or complex trigger.

#### Scenario: Previous pre-confirmation request exists
- **WHEN** the citizen initiates from the home page and the latest request can still be safely replaced before confirmation
- **THEN** the previous request becomes `ABANDONED`, a new request is created, and no progress, certificate selection, or attempt is reused

#### Scenario: Historical terminal request exists
- **WHEN** a completed, failed, not-eligible, abandoned, or otherwise terminal request exists for the DNI
- **THEN** a new request can coexist and all historical rows remain queryable

#### Scenario: Irreversible or uncertain operation exists
- **WHEN** the latest request is confirmed, revoking, or has an uncertain revocation outcome
- **THEN** initiation is rejected with a controlled result without returning or reopening the previous request

#### Scenario: Concurrent new initiations are attempted
- **WHEN** concurrent transactions initiate a new journey for the same DNI
- **THEN** transaction and explicit locking produce at most one new request with an active eligibility attempt without relying on a permanent unique-DNI guard

### Requirement: DNI and sensitive text are stored by purpose
The request SHALL store the citizen DNI once as `CHAR(8)` constrained to eight numeric digits and SHALL store an optional OTHER description as bounded readable text. It MUST NOT create DNI hash, ciphertext, key-version, last-four, encrypted-description, or generated-guard columns. It MUST NOT persist authentication tokens, credentials, biometric data, photographs, complete provider payloads, or document bytes. Full DNI values MUST NOT appear in application logs, error responses, technical endpoints, or URLs.

#### Scenario: Stored request data is inspected
- **WHEN** a contributor queries `certificate_cancellation_request`
- **THEN** one `dni` column clearly identifies the citizen and no duplicate protected representation exists

#### Scenario: Invalid DNI is inserted
- **WHEN** a value other than exactly eight numeric digits is stored as DNI
- **THEN** database or entity validation rejects it

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

### Requirement: Repeatable privacy-limited identity verification
Each ID PerÃº attempt SHALL be stored in `identity_verification` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, provider, status, optional external reference, DNI-match result, start/completion times, optional error or cancellation code, correlation identifier, and creation time. It MUST NOT store a verified-identity hash, biometric data, photographs, provider tokens, credentials, or complete provider responses. `(request_id, attempt_number)` SHALL be unique.

#### Scenario: Identity verification is repeated
- **WHEN** a citizen retries identity verification
- **THEN** a new simplified attempt row is related to the same request without sensitive provider payloads

#### Scenario: Verified identity is queried
- **WHEN** the latest successful verification is requested
- **THEN** the repository can locate it by request, status, and attempt number without a stored identity hash

### Requirement: Idempotent revocation operations
Each technical revocation SHALL be stored separately in `revocation_operation` using numeric identifiers, a numeric request foreign key, and a unique readable `idempotency_key`. It SHALL contain attempt number, status, optional external reference, preparation/submission/response/completion times, one atomic normalized result, optional error code, correlation identifier, and creation/update times. The normalized result SHALL be `SUCCEEDED`, `FAILED`, or `OUTCOME_UNKNOWN` and MUST NOT be `PARTIAL`. The operation MUST NOT contain an optimistic-version or generated open-operation guard and MUST NOT create a new operation automatically after an uncertain outcome. Future concurrent transitions SHALL lock or conditionally update the expected operation state.

#### Scenario: Duplicate idempotency key is used
- **WHEN** a second operation is persisted with an existing idempotency key
- **THEN** the database rejects the duplicate

#### Scenario: Atomic outcome is stored
- **WHEN** the provider confirms all selected certificates were revoked or confirms none were revoked
- **THEN** the operation stores one `SUCCEEDED` or `FAILED` result for the complete selected set

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

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, incremental upgrade through the migration that removes individual results, the effective seven domain tables, Hibernate validation, certificate cardinalities, selection combinations, immutable confirmed selection, optimistic concurrency, UUID uniqueness, operation idempotency, atomic result persistence, restricted deletion, Spanish metadata-comment coverage, and backend startup. Tests MUST prove that existing requests, certificates, selections, revocation operations and receipts survive the forward migration and MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Clean persistence verification is run
- **WHEN** the integration suite starts MySQL from an empty database
- **THEN** all migrations execute and the seven-table atomic model matches the current entity mappings

#### Scenario: Upgrade verification is run
- **WHEN** the suite migrates a V3 database containing representative data to V4
- **THEN** retained domain data remains intact, the individual-result table is absent, and redundant keys used only by that table are removed

#### Scenario: Atomic result matrix is tested
- **WHEN** successful, failed, and uncertain revocation operations are stored
- **THEN** each operation retains one valid general result and no partial or individual result can be persisted

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the seven implemented tables with an updated entity-relationship diagram, a plain-language justification for each table, column descriptions, certificate and request states, indexes, integrity constraints, UUID treatment, selection strategy, atomic-operation strategy, concurrency, migration path, excluded sensitive data, native Spanish comments, and simple inspection queries. It MUST distinguish current implementation facts from still-pending external contracts and MUST explain that all selected certificates share one operation outcome.

#### Scenario: Contributor reviews the atomic model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why selection rows remain, why no individual-result table exists, and how the operation represents all-or-none cancellation

### Requirement: Persistence redesign remains non-functional
This change MUST NOT change the home page or current initiation contract; consume a new certificate service; implement ID Perú, JWT, refresh tokens, selection UI, real or mock revocation execution, PDF or receipt generation, administrative modules, production infrastructure, or final retention policy. It MUST NOT add another database, Redis, event sourcing, CQRS, status catalogs, stored procedures, complex triggers, a selection-only table, cryptographic duplicate columns, or unrelated speculative fields.

#### Scenario: Scope is reviewed
- **WHEN** the implementation diff and runtime routes are inspected
- **THEN** they contain only the incremental persistence model, states, repositories, tests, and documentation required for certificates and individual results, with no new citizen-facing behavior or external call

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
Each request certificate SHALL store whether it is selected and when it became selected. The database SHALL require `selected_at` to be present exactly when `selected` is true. The entity SHALL use optimistic versioning for concurrent edits and SHALL allow repositories to distinguish selected and unselected rows. Selection SHALL permit one, several, or all available certificates, but application behavior MUST reject every selection change after citizen confirmation so that the selected rows remain the evidence of the atomic operation. No table dedicated exclusively to selection or to snapshotting the confirmed set SHALL exist.

#### Scenario: One certificate is selected
- **WHEN** one row among several is selected before confirmation
- **THEN** only that row stores `selected=true` and a selection time

#### Scenario: Several or all certificates are selected
- **WHEN** the citizen selects several or every available row before confirmation
- **THEN** every chosen row is independently queryable as selected and the others remain unselected

#### Scenario: Selection timestamp is inconsistent
- **WHEN** a row is stored as selected without a time or unselected with a selection time
- **THEN** entity or database validation rejects the inconsistent state

#### Scenario: Confirmed selection is modified
- **WHEN** an update attempts to change a request certificate after its request was confirmed
- **THEN** application state validation rejects the update and preserves the atomic set

#### Scenario: Concurrent selection update occurs
- **WHEN** two transactions edit the same certificate from the same version before confirmation
- **THEN** at most one update succeeds and the other receives an optimistic-lock conflict

### Requirement: Certificate identifiers are protected without speculative cryptography
The UUID SHALL be stored once as canonical `CHAR(36)` ASCII data and SHALL be reused through relational references and the justified submitted-value snapshot. It MUST NOT be duplicated as ciphertext, hash, key-version, or public authorization token without a later institutional requirement. Full UUIDs MUST NOT appear in logs, URLs, technical endpoints, unauthenticated responses, or certificate lists shown before successful identity verification.

#### Scenario: Database is inspected by an authorized developer
- **WHEN** a request certificate row is queried directly for diagnosis
- **THEN** its canonical UUID is readable and no speculative cryptographic companion columns exist

#### Scenario: Unauthenticated response is produced
- **WHEN** the initial certificate consultation completes before identity verification
- **THEN** no certificate UUID or certificate-level data is exposed to the client

### Requirement: Concrete repositories support new-journey decisions
The request repository SHALL support lookup and pessimistic locking of the most recent request for a DNI, numeric identifier lookup, latest eligibility and identity attempts, current revocation, available receipt, certificate selection, individual results, and ordered audit history. The initiation use case MUST use these queries only to classify, abandon, or protect prior history; it MUST NOT return the prior request as recovered progress. The backend MUST NOT add a session repository, expiration query, generic custom base repository, eager history, or another persistence table for this behavior.

#### Scenario: Latest replaceable request is inspected
- **WHEN** a new initiation locks the most recent request for the DNI and finds a replaceable pre-confirmation state
- **THEN** the same transaction marks it `ABANDONED` and creates a different request identifier

#### Scenario: Latest protected request is inspected
- **WHEN** the locked request has a confirmed operation in progress or an uncertain result
- **THEN** the repository returns enough state for the use case to reject initiation without exposing historical details

### Requirement: MySQL schema metadata is documented in Spanish
The MySQL persistence schema SHALL store a concise, non-empty Spanish comment for every domain table and every column belonging to those tables. Comments SHALL explain the stable domain or technical purpose of the element, SHALL be directly visible through `INFORMATION_SCHEMA`, and MUST NOT contain real personal data, credentials, secrets, provider payloads, or misleading behavior that is not implemented. Physical table and column names SHALL remain unchanged.

#### Scenario: Contributor inspects table metadata
- **WHEN** a contributor views any of the eight domain tables through MySQL metadata or a compatible database client
- **THEN** the table has a Spanish description that explains its responsibility in the cancellation-request model

#### Scenario: Contributor inspects column metadata
- **WHEN** a contributor views any column belonging to the eight domain tables
- **THEN** `COLUMN_COMMENT` contains a concise Spanish description of that field and no domain column has a blank description

#### Scenario: Sensitive-field description is reviewed
- **WHEN** comments for DNI, certificate UUID, external references, correlation identifiers, storage references, or technical errors are inspected
- **THEN** they describe the purpose of the field without embedding real values, secrets, credentials, or complete external payloads

### Requirement: Schema comments are delivered by forward-only Flyway migration
The comments SHALL be applied by a new incremental Flyway migration after the migrations that create the eight-table schema. Existing successful migrations MUST NOT be edited. The migration SHALL work both when Flyway builds an empty database and when it upgrades a database already at V2, and it MUST preserve all rows, table names, column names, types, attributes, nullability, defaults, keys, indexes, constraints, and relationships.

#### Scenario: Empty database is migrated
- **WHEN** Flyway runs all migrations against an empty MySQL database
- **THEN** the resulting eight-table schema contains complete Spanish table and column comments and Hibernate schema validation succeeds

#### Scenario: Existing V2 database is upgraded
- **WHEN** a MySQL database with the V2 structure and existing fictitious rows executes the comment migration
- **THEN** the rows and relational structure remain unchanged while comments become available

#### Scenario: Migration history is inspected
- **WHEN** checksums and migration files are reviewed after implementation
- **THEN** V1 and V2 remain unchanged and the comment metadata is introduced only by the new forward migration

### Requirement: Comment coverage remains mandatory for future schema changes
Repository documentation and persistence tests SHALL establish that every future domain table or column receives a concise Spanish comment in the migration that creates it. Automated integration verification SHALL fail when an expected domain table has an empty `TABLE_COMMENT` or an expected domain column has an empty `COLUMN_COMMENT`.

#### Scenario: A future migration adds a column
- **WHEN** a new domain column is introduced
- **THEN** the same migration defines its Spanish comment and the metadata coverage test includes the expanded schema

#### Scenario: A comment is accidentally omitted
- **WHEN** the integration suite finds a domain table or column with blank comment metadata
- **THEN** verification fails and identifies the undocumented table or column

