## MODIFIED Requirements

### Requirement: Confirmed selection defines one revocation target
The system SHALL allow the citizen to select exactly one available certificate per request. Confirmation SHALL freeze that certificate as the request's only revocation target, and the future revocation operation SHALL include exactly its UUID. Every unselected certificate MUST remain outside the operation and MUST NOT change as a consequence of it.

#### Scenario: Citizen confirms one certificate
- **WHEN** one certificate is selected from a larger available list and the citizen confirms
- **THEN** exactly that UUID becomes the immutable target and every other certificate remains unaffected

#### Scenario: Selection is changed after confirmation
- **WHEN** application behavior attempts to replace or remove the selected certificate after confirmation
- **THEN** the change is rejected before a revocation operation can use a different target

### Requirement: Revocation processes the single confirmed certificate
The future revocation gateway SHALL submit the confirmed certificate UUID once under one idempotency key. `SUCCEEDED` SHALL mean that certificate was revoked; `FAILED` SHALL mean it was not revoked. The system MUST NOT batch several UUIDs, split a request into per-certificate calls or create a partial citizen result.

#### Scenario: Revocation succeeds
- **WHEN** the provider confirms successful execution for the confirmed UUID
- **THEN** the operation and request record success for that certificate

#### Scenario: Revocation fails
- **WHEN** the provider confirms that the UUID was not revoked
- **THEN** the operation and request record failure without affecting another certificate

#### Scenario: Multiple targets reach the gateway
- **WHEN** an internal caller attempts to build one operation with more than one UUID
- **THEN** the operation is rejected as incompatible with the request model

### Requirement: Uncertain outcome is reconciled without duplication
The system SHALL use `OUTCOME_UNKNOWN` when it cannot confirm whether the single-certificate operation succeeded or failed. It SHALL retain the same operation and idempotency key for reconciliation, MUST NOT create an automatic replacement operation, and MUST prevent an incompatible new cancellation while the outcome remains uncertain.

#### Scenario: Response is lost after submission
- **WHEN** the confirmed UUID was submitted but the response cannot be verified
- **THEN** the operation becomes `OUTCOME_UNKNOWN` without claiming success or failure

#### Scenario: Uncertain operation is retried blindly
- **WHEN** a caller attempts to create another operation for the same confirmed certificate before reconciliation
- **THEN** the system rejects the duplicate and retains the original operation as the reconciliation target

### Requirement: Partial outcome is not a valid domain state
The current domain SHALL support only `SUCCEEDED`, `FAILED`, and `OUTCOME_UNKNOWN` as terminal or reconcilable revocation outcomes. It MUST NOT expose or persist `PARTIAL`, `REVOCATION_PARTIAL`, mixed results or citizen messages that imply partial cancellation within one request.

#### Scenario: Provider returns an incompatible mixed response
- **WHEN** a provider response contains several certificate outcomes for a single-target request
- **THEN** the integration treats it as incompatible rather than normalizing a partial result

#### Scenario: Current states are inspected
- **WHEN** backend enums, API contracts, persistence values and current documentation are reviewed
- **THEN** no active partial revocation state remains

### Requirement: Receipt reflects the single operation outcome
The future receipt SHALL identify the one confirmed certificate and its common operation outcome. It SHALL communicate cancellation only for `SUCCEEDED`, communicate that the certificate was not cancelled for `FAILED`, and avoid any definitive cancellation claim for `OUTCOME_UNKNOWN`. Receipt generation failure MUST remain independent from an already confirmed revocation outcome.

#### Scenario: Successful receipt is generated
- **WHEN** the operation succeeds and receipt generation completes
- **THEN** the receipt identifies the single certificate and records its successful outcome

#### Scenario: Uncertain receipt is requested
- **WHEN** the operation remains `OUTCOME_UNKNOWN`
- **THEN** no final success receipt is produced and the operation remains pending reconciliation
