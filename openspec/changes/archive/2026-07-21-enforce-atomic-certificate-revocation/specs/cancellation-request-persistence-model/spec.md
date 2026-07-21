## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Individual revocation result is persisted per certificate
**Reason**: Atomic execution forbids different confirmed outcomes within the selected set, so per-certificate result rows duplicate the single operation result and add unnecessary schema, mappings and concurrency.

**Migration**: Flyway V4 removes `certificate_revocation_result`; current rows are disposable because real revocation is not implemented. If relevant data is discovered before application, migration stops until a preservation strategy is approved.

### Requirement: Overall revocation outcome is derived from individual results
**Reason**: `PARTIAL` and aggregation from independent rows contradict the all-or-none business rule.

**Migration**: Remove the result calculator and partial enums/states. Read the authoritative result directly from `revocation_operation.normalized_result` and propagate it to the request using controlled transitions.

