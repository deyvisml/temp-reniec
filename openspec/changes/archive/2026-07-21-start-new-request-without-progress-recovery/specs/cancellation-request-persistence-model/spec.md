## MODIFIED Requirements

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

### Requirement: Integration tests exercise MySQL behavior
The persistence suite SHALL use disposable MySQL Testcontainers and SHALL verify Flyway from empty, all eight tables, readable DNI persistence, entity relationships, creation and update, explicit concurrent access behavior, attempt uniqueness, certificate selection integrity, revocation idempotency, individual results, receipt-code uniqueness, ordered audit, historical-request preservation, new-request initiation without recovery, protected in-flight operations, basic integrity, and backend startup. Tests MUST NOT require a manually installed MySQL server or external service.

#### Scenario: Persistence verification is run
- **WHEN** a contributor runs the documented backend verification command with a container runtime available
- **THEN** the eight-table relational schema and the non-recovering new-request behavior are verified against real MySQL semantics

### Requirement: Data-model documentation matches the implementation
The repository SHALL document the eight implemented tables with a compact entity-relationship diagram, a plain-language justification for each table, column descriptions, controlled states, indexes, integrity constraints, the historical-request strategy, the explicit plaintext-DNI decision, excluded sensitive data, and simple inspection queries. It MUST clearly distinguish progress within the current request from cross-entry recovery, state that a new home-page initiation creates a new request, and explain that historical constancias remain stored but are not reopened automatically.

#### Scenario: Contributor reviews the model
- **WHEN** a contributor opens the data-model documentation
- **THEN** the contributor can explain why every table and column exists, why history is retained, and why prior progress is not restored

## ADDED Requirements

### Requirement: Concrete repositories support new-journey decisions
The request repository SHALL support lookup and pessimistic locking of the most recent request for a DNI, numeric identifier lookup, latest eligibility and identity attempts, current revocation, available receipt, certificate selection, individual results, and ordered audit history. The initiation use case MUST use these queries only to classify, abandon, or protect prior history; it MUST NOT return the prior request as recovered progress. The backend MUST NOT add a session repository, expiration query, generic custom base repository, eager history, or another persistence table for this behavior.

#### Scenario: Latest replaceable request is inspected
- **WHEN** a new initiation locks the most recent request for the DNI and finds a replaceable pre-confirmation state
- **THEN** the same transaction marks it `ABANDONED` and creates a different request identifier

#### Scenario: Latest protected request is inspected
- **WHEN** the locked request has a confirmed operation in progress or an uncertain result
- **THEN** the repository returns enough state for the use case to reject initiation without exposing historical details

## REMOVED Requirements

### Requirement: Concrete repositories support minimum recovery queries
**Reason**: The product no longer restores an unfinished request or previous constancia when the citizen enters again; request history is retained only for traceability and safety decisions.

**Migration**: Replace resumable-status lookup with a locked latest-request lookup used to abandon safe pre-confirmation history or block unsafe concurrent/irreversible operations. Do not create session or recovery infrastructure.
