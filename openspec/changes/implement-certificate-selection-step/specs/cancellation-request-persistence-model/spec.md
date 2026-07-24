## ADDED Requirements

### Requirement: Authenticated certificate loading activates the reserved certificate model
After verified identity, the application SHALL use `cancellation_request_certificate` as the sole persisted snapshot of the second-service list. Completion SHALL create zero, one or many request-owned rows atomically and SHALL transition the request through `CHECKING_CERTIFICATE_LIST`, `CERTIFICATES_AVAILABLE` or `NO_CERTIFICATES_AVAILABLE` as appropriate. Initial availability attempts MUST remain unrelated to these rows.

#### Scenario: Authenticated list is persisted
- **WHEN** the second service returns a valid non-empty collection
- **THEN** request certificate rows are created without a foreign key or source reference to the initial availability check

#### Scenario: Authenticated list is empty
- **WHEN** the second service confirms no current certificates
- **THEN** zero rows are created and the request records that continuation is unavailable

### Requirement: Selection persistence is transactionally authoritative
The application SHALL persist a submitted complete selection by updating the existing `selected` and `selected_at` columns in one transaction. It SHALL validate ownership and availability before changing any row, preserve optimistic concurrency through the existing version column and create no selection table.

#### Scenario: Selection transaction fails validation
- **WHEN** any submitted UUID is unknown, duplicated, unavailable or belongs to another request
- **THEN** no certificate row changes selection state

#### Scenario: Selection transaction succeeds
- **WHEN** the selected UUID set is non-empty and fully valid
- **THEN** selected rows and deselected rows match exactly the submitted set and the request advances to `CERTIFICATES_SELECTED`
