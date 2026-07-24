## ADDED Requirements

### Requirement: Step 2 always presents the authenticated certificate list
The frontend SHALL render step 2 after verified identity using `docs/ui-reference/step-2.png` as the composition reference and the five-step shared stepper. It SHALL render the selection experience for one or many certificates, and MUST NOT skip the view or auto-select the only certificate.

#### Scenario: One certificate is available
- **WHEN** the authenticated listing contains one certificate
- **THEN** step 2 displays that certificate unselected and requires an explicit citizen selection

#### Scenario: Several certificates are available
- **WHEN** the authenticated listing contains multiple certificates
- **THEN** every certificate is distinguishable by order number, creation date and UUID and can be selected independently

### Requirement: Selection controls are accessible and responsive
Each certificate SHALL have a keyboard-operable native selection control with an associated label. The view SHALL provide select-all behavior, selected-count feedback, visible focus, non-color-only states and an `aria-live` status. It SHALL remain usable without mandatory horizontal scrolling at supported mobile widths.

#### Scenario: Citizen changes selection with a keyboard
- **WHEN** focus reaches a certificate checkbox and Space is pressed
- **THEN** its state and the announced selected count update without a pointer

#### Scenario: Citizen uses a narrow viewport
- **WHEN** step 2 is rendered at a supported mobile width
- **THEN** certificate fields and actions remain readable, operable and ordered without clipping

### Requirement: At least one certificate is required to continue
The primary action SHALL remain unavailable until at least one currently available certificate is selected and no submission is active. The interface SHALL show the selected quantity and SHALL prevent duplicate submissions.

#### Scenario: No certificate is selected
- **WHEN** the citizen has not selected any row
- **THEN** Continue cannot submit and the view explains that at least one selection is required

#### Scenario: Several certificates are selected
- **WHEN** the citizen selects two or more available rows
- **THEN** the counter reflects the exact quantity and one Continue action submits the complete set once

### Requirement: The backend validates and persists the complete selected set
The selection endpoint SHALL accept a non-empty set of canonical UUIDs and SHALL derive the request from the authenticated session. In one transaction it SHALL reject duplicates, unknown UUIDs, unavailable certificates, cross-request certificates and disallowed request states, then replace the persisted selection flags and timestamps. It MUST NOT trust client certificate metadata.

#### Scenario: Valid subset is submitted
- **WHEN** all submitted UUIDs belong to currently available certificates of the active request
- **THEN** exactly those rows become selected and the request becomes `CERTIFICATES_SELECTED`

#### Scenario: Arbitrary UUID is injected
- **WHEN** the submitted set includes a UUID absent from the active request
- **THEN** the entire update is rejected and no prior selection is partially changed

#### Scenario: Duplicate UUID is submitted
- **WHEN** the payload repeats a UUID
- **THEN** validation rejects the request rather than silently normalizing ambiguous input

### Requirement: Selection updates are idempotent and concurrency-safe
Repeating the same selected set SHALL return the same successful state. Concurrent conflicting writes SHALL be detected through existing optimistic versions and returned as a controlled conflict that instructs the client to reload; no automatic retry SHALL overwrite another tab's decision.

#### Scenario: Same selection is submitted twice
- **WHEN** a completed request is repeated with the identical UUID set
- **THEN** the endpoint succeeds without duplicating rows or changing selection timestamps unnecessarily

#### Scenario: Two tabs submit different sets concurrently
- **WHEN** both transactions started from the same certificate versions
- **THEN** one completes and the other receives a conflict without a partially mixed set

### Requirement: A valid selection alone authorizes the next step
The protected-flow state SHALL expose certificate selection as the current step until persistence succeeds. After success it SHALL authorize the reason step, while direct access to reason or later steps before a valid selection remains blocked. The reason view itself is outside this change and SHALL be represented only by a controlled transition.

#### Scenario: Citizen continues successfully
- **WHEN** the backend confirms a non-empty persisted selection
- **THEN** the shared stepper marks step 2 complete and the session context authorizes step 3

#### Scenario: Citizen attempts to skip selection
- **WHEN** the browser requests a future step while the request lacks a valid persisted selection
- **THEN** the application redirects or renders the backend-authorized selection step

### Requirement: Empty and technical outcomes have distinct recovery actions
An empty list SHALL show a terminal no-current-certificates message without retrying identity. Timeout, unavailability and malformed provider responses SHALL retain the authenticated selection stage and offer only safe retry or logout actions. Session expiry SHALL return to the public home through the established session behavior.

#### Scenario: Second service returns empty
- **WHEN** the authenticated listing response confirms zero certificates
- **THEN** the citizen sees that no certificates are currently available and cannot continue

#### Scenario: Listing service fails technically
- **WHEN** the API returns timeout, unavailable or malformed-provider error
- **THEN** the citizen sees a non-technical message and can retry without repeating ID Perú authentication
