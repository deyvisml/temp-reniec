## MODIFIED Requirements

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
