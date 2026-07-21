# atomic-certificate-revocation Specification

## Purpose
TBD - created by archiving change enforce-atomic-certificate-revocation. Update Purpose after archive.
## Requirements
### Requirement: Confirmed selection defines the atomic revocation set
The system SHALL allow the citizen to select one, several, or all available certificates. Confirmation SHALL freeze the selected set for that request, and the revocation operation SHALL include exactly every selected UUID in one atomic unit. Unselected certificates MUST remain outside the operation and MUST NOT change as a consequence of it.

#### Scenario: Citizen selects a subset
- **WHEN** the citizen confirms two certificates from a larger available list
- **THEN** exactly those two UUIDs form the atomic operation and every unselected certificate remains unaffected

#### Scenario: Selection is changed after confirmation
- **WHEN** application behavior attempts to add or remove a selected certificate after confirmation
- **THEN** the change is rejected before a revocation operation can use a different set

### Requirement: Revocation is all-or-none for the selected set
The future revocation gateway SHALL submit the complete selected UUID list once under one idempotency key and SHALL normalize only an atomic response. `SUCCEEDED` SHALL mean every selected certificate was revoked; `FAILED` SHALL mean no selected certificate was revoked. The system MUST NOT implement the batch as independent per-certificate calls, accept a confirmed mixed outcome, or attempt compensating revocations.

#### Scenario: Atomic revocation succeeds
- **WHEN** the provider confirms successful execution of the selected list
- **THEN** the operation and request record success for the complete selected set

#### Scenario: Atomic revocation fails
- **WHEN** the provider confirms that the selected list was not executed
- **THEN** the operation and request record failure and no selected certificate is represented as revoked

#### Scenario: Provider offers only independent outcomes
- **WHEN** an external contract can revoke some UUIDs while rejecting others
- **THEN** that contract is rejected as incompatible rather than adapted into a partial citizen result

### Requirement: Uncertain outcome is reconciled without duplication
The system SHALL use `OUTCOME_UNKNOWN` when it cannot confirm whether the atomic operation succeeded or failed. It SHALL retain the same operation and idempotency key for reconciliation, MUST NOT create an automatic replacement operation, and MUST prevent an incompatible new cancellation while the outcome remains uncertain.

#### Scenario: Response is lost after submission
- **WHEN** the selected UUID list was submitted but the response cannot be verified
- **THEN** the operation becomes `OUTCOME_UNKNOWN` without claiming that any individual certificate succeeded or failed

#### Scenario: Uncertain operation is retried blindly
- **WHEN** a caller attempts to create another operation for the same confirmed selection before reconciliation
- **THEN** the system rejects the duplicate and retains the original operation as the reconciliation target

### Requirement: Partial outcome is not a valid domain state
The current domain SHALL support only `SUCCEEDED`, `FAILED`, and `OUTCOME_UNKNOWN` as terminal or reconcilable revocation outcomes. It MUST NOT expose or persist `PARTIAL`, `REVOCATION_PARTIAL`, mixed per-certificate results, or citizen messages that imply confirmed partial cancellation.

#### Scenario: Mixed provider response is received
- **WHEN** a provider response appears to report both successful and failed certificates
- **THEN** the integration does not normalize it as a valid partial result and treats the contract or response as incompatible

#### Scenario: Current states are inspected
- **WHEN** backend enums, API contracts, persistence values and current documentation are reviewed
- **THEN** no active partial revocation state remains

### Requirement: Receipt reflects one atomic outcome
The future receipt SHALL identify the confirmed selected certificates and one common operation outcome. It SHALL communicate that all selected certificates were cancelled only for `SUCCEEDED`, that none were cancelled for `FAILED`, and SHALL avoid any definitive cancellation claim for `OUTCOME_UNKNOWN`. Receipt generation failure MUST remain independent from an already confirmed revocation outcome.

#### Scenario: Successful receipt is generated
- **WHEN** an atomic operation succeeds and receipt generation completes
- **THEN** the receipt lists the selected set and records one successful outcome for all of it

#### Scenario: Uncertain receipt is requested
- **WHEN** the operation remains `OUTCOME_UNKNOWN`
- **THEN** no final success receipt is produced and the system directs the operation to reconciliation

