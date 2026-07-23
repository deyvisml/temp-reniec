# Cancellation Request Persistence Model Specification

## Purpose

Define the authoritative, simplified MySQL persistence model centered on the citizen's certificate cancellation request and its justified repeatable or independently managed records.
## Requirements
### Requirement: Clean authoritative schema from an empty database
Flyway SHALL build the seven-table atomic model and SHALL apply a new forward migration after V4 that renames the initial consultation persistence from eligibility to availability, converts unambiguous legacy result values, and removes the incorrect relationship from `cancellation_request_certificate` to the initial check. Previously successful migrations MUST remain unchanged. The resulting schema SHALL match Hibernate mappings and contain no request-session, selection-only, listing-attempt, status-catalog, generated-guard or individual-revocation-result table.

#### Scenario: Empty database is initialized
- **WHEN** the backend starts against an empty MySQL database
- **THEN** Flyway applies the complete history and Hibernate validates the seven-table schema with availability naming and no initial-check foreign key on certificates

#### Scenario: Existing V4 database is upgraded
- **WHEN** the new migration runs over representative requests, checks and certificates
- **THEN** all rows and certificate details survive while the initial check is renamed and its obsolete certificate relationship is removed

#### Scenario: Migration history is inspected
- **WHEN** a contributor reviews Flyway files and checksums
- **THEN** V1 through V4 remain unchanged and the correction exists only in the new reproducible migration

### Requirement: Cancellation request is the persistent root
The system SHALL persist each citizen journey in `certificate_cancellation_request` with numeric identifier, full DNI, current request status, current availability result, reason, optional OTHER description, confirmation time, final outcome, creation time and update time. The availability result SHALL express only whether existence was confirmed or could not be determined; it MUST NOT imply that a detailed certificate list has been obtained.

#### Scenario: Positive initial result is inspected
- **WHEN** the first service confirms availability
- **THEN** the request stores `AVAILABLE` and `PENDING_IDENTITY_VERIFICATION` while it has no certificate rows

#### Scenario: Negative initial result is inspected
- **WHEN** the first service confirms absence
- **THEN** the request stores `NOT_AVAILABLE` and `NO_CERTIFICATES_AVAILABLE` without creating certificate rows

#### Scenario: Technical result is inspected
- **WHEN** the first service is inconclusive, unavailable, times out or fails
- **THEN** the request preserves a non-negative normalized result and cannot advance

### Requirement: Backend-controlled request states
The request status SHALL remain a backend-controlled string and SHALL distinguish `CHECKING_AVAILABILITY`, `NO_CERTIFICATES_AVAILABLE`, `PENDING_IDENTITY_VERIFICATION`, `IDENTITY_VERIFIED`, `AUTHENTICATED_PENDING_CERTIFICATE_LIST`, `CERTIFICATES_AVAILABLE`, and `CERTIFICATES_SELECTED` before the existing confirmation and atomic-revocation states. `PENDING_IDENTITY_VERIFICATION` SHALL mean only that existence was confirmed. `CERTIFICATES_AVAILABLE` SHALL mean that the future second service returned and persisted a non-empty detailed list. No status catalog or duplicate current-step column SHALL be created.

#### Scenario: Initial availability is positive
- **WHEN** the first check returns `AVAILABLE`
- **THEN** the request becomes `PENDING_IDENTITY_VERIFICATION` and not `CERTIFICATES_AVAILABLE`

#### Scenario: Authentication completes
- **WHEN** identity verification later succeeds
- **THEN** the request can become `AUTHENTICATED_PENDING_CERTIFICATE_LIST` before any certificate is persisted

#### Scenario: Detailed list is obtained
- **WHEN** the future second service returns a non-empty list and it is persisted
- **THEN** the request can become `CERTIFICATES_AVAILABLE`

#### Scenario: Detailed list becomes empty after positive existence
- **WHEN** the future second service returns no certificate after authentication
- **THEN** the request can become `NO_CERTIFICATES_AVAILABLE` without representing authentication failure

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
Each initial certificate-availability attempt SHALL be stored in `certificate_availability_check` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, technical status, normalized availability result, optional external reference, request and response times, optional error code, correlation identifier and creation time. It MUST NOT contain or own certificate rows, counts, order numbers, creation dates, UUIDs or complete provider payloads. `(request_id, attempt_number)` SHALL remain unique.

#### Scenario: First service returns true
- **WHEN** a completed attempt confirms at least one available certificate
- **THEN** the attempt stores `AVAILABLE` and no certificate row is created

#### Scenario: First service returns false
- **WHEN** a completed attempt confirms no available certificates
- **THEN** the attempt stores `NOT_AVAILABLE` and no certificate row is created

#### Scenario: Initial query fails
- **WHEN** the attempt is inconclusive, unavailable, times out or fails technically
- **THEN** its distinct result or error is persisted and not converted to `NOT_AVAILABLE`

#### Scenario: Attempt number is reused
- **WHEN** two availability rows use the same request and attempt number
- **THEN** the database rejects the duplicate

### Requirement: Repeatable privacy-limited identity verification
Each ID Perú attempt SHALL be stored in `identity_verification` using numeric identifiers and a numeric request foreign key. It SHALL contain attempt number, provider, status, optional external reference, DNI-match result, start/completion times, optional error or cancellation code, correlation identifier, and creation time. It MUST NOT store a verified-identity hash, biometric data, photographs, provider tokens, credentials, or complete provider responses. `(request_id, attempt_number)` SHALL be unique.

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
All seven domain tables SHALL use numeric primary keys and matching numeric foreign keys where a current relationship exists. The database SHALL reject orphan rows and duplicate certificate UUIDs within one request, SHALL retain history by omitting cascading deletion, and SHALL keep focused uniqueness constraints and indexes for documented queries. `cancellation_request_certificate` MUST reference its request but MUST NOT reference `certificate_availability_check`, because the initial service never obtains certificates.

#### Scenario: Initial attempt is deleted or renamed during migration
- **WHEN** the corrected schema is inspected
- **THEN** no certificate foreign key, source index or source column points to the availability check

#### Scenario: Duplicate certificate is inserted
- **WHEN** the same canonical UUID is inserted twice for one request by future listing behavior
- **THEN** the database rejects the duplicate

#### Scenario: Request deletion is attempted with history
- **WHEN** a request with checks or certificates is deleted directly
- **THEN** foreign-key integrity prevents accidental loss of persisted history

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, incremental V4-to-current upgrade, seven domain tables, Hibernate validation, availability-result conversion, preservation of existing requests and certificates, removal of the initial-check certificate relationship, certificate cardinalities and selection behavior, atomic revocation persistence, Spanish comment coverage and backend startup. It SHALL verify that every initial availability scenario leaves zero certificate rows for its newly created request.

#### Scenario: Clean persistence verification is run
- **WHEN** the integration suite starts MySQL from an empty database
- **THEN** all migrations execute and the corrected seven-table model matches current mappings

#### Scenario: Upgrade verification is run
- **WHEN** the suite upgrades V4 data containing initial checks and certificate rows
- **THEN** data remains intact, results use corrected naming and certificate rows no longer depend on the initial check

#### Scenario: Initial-query persistence matrix is tested
- **WHEN** positive, negative, inconclusive, unavailable, timeout and technical outcomes are persisted
- **THEN** each attempt and request has the correct state and no certificate row is inserted

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the seven implemented tables with an updated entity-relationship diagram, plain-language justification, column descriptions, availability and request states, indexes, integrity constraints, UUID treatment, selection and atomic-operation strategies, migration path, excluded sensitive data, Spanish comments and inspection queries. It SHALL explain that `certificate_availability_check` belongs to the home query, while `cancellation_request_certificate` is reserved for the future authenticated listing and is not populated by that check.

#### Scenario: Contributor explains the two stages
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why a positive availability attempt has zero certificate rows and why detailed certificates remain a separate later responsibility

### Requirement: Persistence redesign remains non-functional
This change SHALL correct the persistence vocabulary, migration history, request states and initial-query relationships required by the implemented home flow. It MUST NOT implement ID Perú, the second listing service or its attempt model, selection endpoints or UI, real revocation execution, PDF generation, administration, production infrastructure or speculative external contracts. It MUST NOT remove `cancellation_request_certificate` or add a placeholder listing table.

#### Scenario: Scope is reviewed
- **WHEN** runtime routes, entities and migrations are inspected
- **THEN** only the existing initial query changes behavior and the detailed-certificate structures remain unused by that operation

### Requirement: Consulted certificates belong to a cancellation request
The system SHALL retain `cancellation_request_certificate` for each detailed current certificate obtained in the future authenticated listing, with its cancellation request, order number, emission creation time, canonical UUID, availability status, consultation time, selection flag, optional selection time, optimistic version, creation time and update time. A request SHALL support zero, one or many certificate rows. `(request_id, certificate_uuid)` MUST remain unique. The entity MUST NOT require or expose an initial availability-attempt relationship.

#### Scenario: Initial query completes
- **WHEN** the first service returns any functional or technical result
- **THEN** no request certificate is created

#### Scenario: Existing certificate survives migration
- **WHEN** an existing V4 certificate row is upgraded
- **THEN** its request, order number, emission time, UUID, availability, selection and audit timestamps remain intact after the obsolete source relationship is removed

#### Scenario: Future listing returns certificates
- **WHEN** the second service is implemented in a later change
- **THEN** its certificate rows can use this entity without pretending they came from the initial availability check

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
The request repository SHALL support locking the most recent request for a DNI and the availability-check repository SHALL support the latest initial attempt. Certificate repositories SHALL remain available for later authenticated listing and selection, but the initiation use case MUST NOT invoke them. The backend MUST NOT add a listing repository without a corresponding implemented service, a session repository, a generic custom base, eager history or another table for this correction.

#### Scenario: Initial request is processed
- **WHEN** the availability use case prepares or finalizes an attempt
- **THEN** it uses request and availability-check repositories only

#### Scenario: Persistence dependencies are inspected
- **WHEN** the initiation service and coordinator are reviewed
- **THEN** they contain no certificate-entity or certificate-repository dependency

### Requirement: MySQL schema metadata is documented in Spanish
The MySQL persistence schema SHALL store a concise, non-empty Spanish comment for every table and column in the effective seven-table domain model. Comments SHALL use the corrected availability terminology, SHALL explain that detailed certificates are populated only by the future post-authentication listing, and MUST NOT contain real personal data, credentials, secrets, provider payloads or unimplemented behavior.

#### Scenario: Availability metadata is inspected
- **WHEN** a contributor views `certificate_availability_check` and `availability_result` through MySQL metadata
- **THEN** their Spanish comments describe the boolean existence query and its normalized result

#### Scenario: Certificate metadata is inspected
- **WHEN** a contributor views `cancellation_request_certificate`
- **THEN** no column or comment attributes its origin to the initial availability attempt

### Requirement: Schema comments are delivered by forward-only Flyway migration
The corrected comments SHALL be applied in the same new forward migration that performs the availability renames and relationship removal after V4. Existing successful migrations MUST NOT be edited. The migration SHALL work from an empty database and when upgrading a database already at V4, and it SHALL preserve all retained rows, keys and constraints not explicitly superseded by this correction.

#### Scenario: Empty database is migrated
- **WHEN** Flyway runs all migrations against an empty MySQL database
- **THEN** the resulting seven-table schema contains complete current Spanish metadata and Hibernate validation succeeds

#### Scenario: Existing V4 database is upgraded
- **WHEN** representative V4 data executes the correction migration
- **THEN** retained data remains unchanged while names, relationships and comments reflect the two-service rule

### Requirement: Comment coverage remains mandatory for future schema changes
Persistence tests SHALL require every current or future domain table and column to have a concise Spanish comment in the migration that creates or changes it. Coverage SHALL evaluate the effective seven-table schema after the correction and fail for obsolete, blank or misleading metadata.

#### Scenario: Correction migration is verified
- **WHEN** metadata coverage runs after the availability migration
- **THEN** all current tables and columns have non-empty Spanish comments and no assertion expects the removed source column

#### Scenario: Future listing migration adds structures
- **WHEN** a later change adds a justified listing-attempt table or relation
- **THEN** that same migration supplies Spanish comments and extends metadata coverage

### Requirement: Identity attempts persist only necessary transient security state
`identity_verification` SHALL remain the persistence record for each ID Perú attempt and SHALL store request, attempt number, provider, real/mock mode, status, safe external reference, state hash, state expiry/consumption, protected PKCE verifier, session_state when valid, safe verified-subject reference, DNI match result, normalized error, correlation and technical timestamps. It SHALL NOT store provider tokens, authorization codes, plaintext state/verifier, client secret, biometric data, photographs or complete responses.

#### Scenario: Attempt starts
- **WHEN** identity verification is prepared
- **THEN** one attempt row with unique request attempt number, unique state hash, expiry and protected verifier is committed

#### Scenario: Attempt completes
- **WHEN** callback processing reaches a terminal result
- **THEN** status, completion and match/error fields are recorded and the protected verifier is cleared

#### Scenario: Persistence is inspected
- **WHEN** tables are queried after real or mock authentication
- **THEN** no prohibited provider artifact or unnecessary citizen claim is present

### Requirement: Callback consumption is enforced atomically
The persistence layer SHALL provide an atomic conditional operation that consumes only a started, unconsumed and unexpired attempt selected by state hash. A unique state constraint and unique `(request_id, attempt_number)` constraint SHALL prevent duplicates without adding an event-sourcing model or a session table.

#### Scenario: Concurrent callbacks arrive
- **WHEN** two transactions submit the same valid state simultaneously
- **THEN** exactly one consumes the attempt and the other observes a replay conflict

#### Scenario: Migration runs on existing data
- **WHEN** Flyway applies the identity extension over a schema through V5
- **THEN** existing rows remain valid and new uniqueness rules do not discard prior request history

### Requirement: Temporary flow authorization is represented on the verified attempt
The verified attempt SHALL store only the hash, validity and invalidation timestamp/reason needed to validate the current short-lived flow authorization. It MUST NOT create `cancellation_request_session` or any replacement session table.

#### Scenario: Authorization is issued
- **WHEN** an identity attempt becomes verified and matching
- **THEN** its authorization hash and expiry are stored in the same transaction as the request transition

#### Scenario: Authorization is invalidated
- **WHEN** logout, abandonment, expiration or completion occurs
- **THEN** the attempt records invalidation and no refresh or recovery record is created

### Requirement: Identity schema remains documented and reproducible
The incremental Flyway migration SHALL add Spanish table/column comments, foreign keys, uniqueness and search indexes matching JPA. It SHALL build correctly both from an empty database and from the current V5 schema, and Hibernate validation SHALL succeed.

#### Scenario: Empty database starts
- **WHEN** Flyway executes V1 through the new migration on MySQL 8.4
- **THEN** the resulting identity schema matches the entities and includes its Spanish descriptions

#### Scenario: Existing V5 database upgrades
- **WHEN** the new migration runs over the current development schema
- **THEN** no correctly modelled cancellation, certificate or revocation data is removed or redesigned
